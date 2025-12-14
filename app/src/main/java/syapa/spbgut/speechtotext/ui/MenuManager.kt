package syapa.spbgut.speechtotext.ui

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.R

class MenuManager(private val activity: MainActivity) {

    fun showMenu(anchor: View) {
        val popup = PopupMenu(activity, anchor)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_clear -> {
                    activity.clearText()
                    true
                }
                R.id.menu_save -> {
                    activity.saveToFile()
                    true
                }
                R.id.menu_punctuate -> {
                    activity.processTextWithModel()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}