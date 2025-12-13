package syapa.spbgut.speechtotext.listeners

import android.text.Editable
import android.text.TextWatcher
import syapa.spbgut.speechtotext.MainActivity

class TextChangeListener(private val activity: MainActivity) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: Editable?) {
        activity.lastCursorPosition = activity.etResult.selectionStart
    }
}