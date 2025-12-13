package syapa.spbgut.speechtotext.ui

import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import syapa.spbgut.speechtotext.MainActivity
import syapa.spbgut.speechtotext.R

class MenuManager(private val activity: MainActivity) {

    fun showMenu(anchor: View) {
        val popup = PopupMenu(activity, anchor)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_clear -> {
                    activity.clearText()
                    Toast.makeText(activity, "Текст очищен", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.menu_save -> {
                    activity.saveToFile()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }
}