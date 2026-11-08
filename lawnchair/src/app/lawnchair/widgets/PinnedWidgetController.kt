package app.lawnchair.widgets

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.model.data.LauncherAppWidgetInfo
import com.android.launcher3.widget.LauncherAppWidgetHostView

/**
 * One interactive widget, anchored in the launcher's DragLayer rather than in a
 * horizontally scrolling CellLayout. This is NOT a system overlay: it only
 * belongs to the launcher Activity.
 *
 * An invisible placeholder remains in the owning CellLayout to keep occupancy,
 * item lookup and model persistence stable.
 */
class PinnedWidgetController(private val launcher: LawnchairLauncher) {
    companion object {
        const val OPTION_PINNED = 1 shl 28

        fun isPinned(info: LauncherAppWidgetInfo): Boolean =
            info.options and OPTION_PINNED != 0
    }

    private class Pinned(
        val info: LauncherAppWidgetInfo,
        val widget: LauncherAppWidgetHostView,
        val placeholder: View,
        val originalParent: ViewGroup,
        val originalIndex: Int,
        val originalParams: ViewGroup.LayoutParams,
    )

    private var current: Pinned? = null

    fun getPinnedWidget(appWidgetId: Int): LauncherAppWidgetHostView? =
        current?.takeIf { it.info.appWidgetId == appWidgetId }?.widget

    fun pinBoundWidget(info: LauncherAppWidgetInfo) {
        // Pending widgets need Launcher3's normal restore/configuration flow.
        if (info.appWidgetId < 0 ||
            info.container != CONTAINER_DESKTOP ||
            info.restoreStatus != LauncherAppWidgetInfo.RESTORE_COMPLETED
        ) return

        if (current?.info?.appWidgetId == info.appWidgetId) return
        // Reloads should not leave the previous overlay stranded.
        unpinActive(clearFlag = true)

        val widget = launcher.workspace.getWidgetForAppWidgetId(info.appWidgetId) ?: return
        val originalParent = widget.parent as? ViewGroup ?: return
        val originalIndex = originalParent.indexOfChild(widget)
        if (originalIndex < 0) return
        val originalParams = widget.layoutParams

        originalParent.removeView(widget)
        val placeholder = View(launcher).apply {
            tag = info
            visibility = View.INVISIBLE
        }
        originalParent.addView(placeholder, originalIndex, originalParams)
        launcher.dragLayer.addView(
            widget,
            FrameLayout.LayoutParams(1, 1).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
            },
        )

        current = Pinned(info, widget, placeholder, originalParent, originalIndex, originalParams)
        placeholder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updatePosition()
        }
        placeholder.post { updatePosition() }
        updateVisibility(launcher.stateManager.state)
    }

    fun unpinActive(clearFlag: Boolean) {
        val entry = current ?: return
        current = null
        val widget = entry.widget
        (widget.parent as? ViewGroup)?.removeView(widget)

        if (entry.placeholder.parent === entry.originalParent) {
            entry.originalParent.removeView(entry.placeholder)
            entry.originalParent.addView(
                widget,
                entry.originalIndex.coerceAtMost(entry.originalParent.childCount),
                entry.originalParams,
            )
        }
        if (clearFlag) {
            entry.info.options = entry.info.options and OPTION_PINNED.inv()
            launcher.modelWriter.updateItemInDatabase(entry.info)
        }
        launcher.workspace.requestLayout()
    }

    fun clearOnDestroy() {
        // No database mutation: a new Launcher instance rebinds the same widget.
        unpinActive(clearFlag = false)
    }

    fun updateVisibility(state: LauncherState) {
        current?.widget?.visibility =
            if (state == LauncherState.NORMAL && !launcher.workspace.isOverlayShown) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun updatePosition() {
        val entry = current ?: return
        val page = launcher.workspace.getScreenWithId(entry.info.screenId) ?: return
        val marker = entry.placeholder
        if (!marker.isLaidOut || marker.width <= 0 || marker.height <= 0) return

        val markerXY = IntArray(2)
        val pageXY = IntArray(2)
        val workspaceXY = IntArray(2)
        val dragXY = IntArray(2)
        marker.getLocationOnScreen(markerXY)
        page.getLocationOnScreen(pageXY)
        launcher.workspace.getLocationOnScreen(workspaceXY)
        launcher.dragLayer.getLocationOnScreen(dragXY)

        // Cancel the page's scrolling offset so the widget stays in the same
        // viewport position no matter which home page is selected.
        val x = workspaceXY[0] - dragXY[0] + markerXY[0] - pageXY[0]
        val y = workspaceXY[1] - dragXY[1] + markerXY[1] - pageXY[1]

        val params = entry.widget.layoutParams as? FrameLayout.LayoutParams ?: return
        if (params.width != marker.width || params.height != marker.height ||
            params.leftMargin != x || params.topMargin != y
        ) {
            params.width = marker.width
            params.height = marker.height
            params.leftMargin = x
            params.topMargin = y
            entry.widget.layoutParams = params
        }
    }

    /** Invoked from a homescreen long-press action, never over other apps. */
    fun showPicker() {
        val choices = mutableListOf<LauncherAppWidgetInfo>()
        launcher.workspace.mapOverItems { info, view ->
            if (info is LauncherAppWidgetInfo &&
                view is LauncherAppWidgetHostView &&
                info.container == CONTAINER_DESKTOP &&
                info.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED
            ) {
                choices.add(info)
            }
            false
        }

        val pinned = current?.info
        if (pinned != null && choices.none { it.appWidgetId == pinned.appWidgetId }) {
            choices.add(pinned)
        }
        if (choices.isEmpty()) {
            Toast.makeText(launcher, R.string.moonchair_no_widgets, Toast.LENGTH_LONG).show()
            return
        }

        val appWidgetManager = AppWidgetManager.getInstance(launcher)
        val labels = choices.map { info ->
            val label = appWidgetManager.getAppWidgetInfo(info.appWidgetId)
                ?.loadLabel(launcher.packageManager)
                ?: info.providerName?.shortClassName ?: "Widget"
            "$label (#${info.appWidgetId})" +
                if (info.appWidgetId == pinned?.appWidgetId) " \u2713" else ""
        }.toMutableList()
        if (pinned != null) labels.add(launcher.getString(R.string.moonchair_unpin_widget))

        AlertDialog.Builder(launcher)
            .setTitle(R.string.moonchair_pin_widget)
            .setItems(labels.toTypedArray()) { _, selected ->
                if (selected == choices.size) {
                    unpinActive(clearFlag = true)
                } else {
                    val info = choices[selected]
                    if (current?.info?.appWidgetId != info.appWidgetId) {
                        unpinActive(clearFlag = true)
                        info.options = info.options or OPTION_PINNED
                        launcher.modelWriter.updateItemInDatabase(info)
                        pinBoundWidget(info)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
