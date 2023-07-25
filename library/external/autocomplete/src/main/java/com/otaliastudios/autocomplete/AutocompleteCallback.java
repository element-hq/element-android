package com.otaliastudios.autocomplete;

import android.text.Editable;

import androidx.annotation.NonNull;

/**
 * Optional callback to be passed to {@link Autocomplete.Builder}.
 */
public interface AutocompleteCallback<T> {

    /**
     * Called when an item inside your list is clicked.
     * This works if your presenter has dispatched a click event.
     * At this point you can edit the text, e.g. {@code editable.append(item.toString())}.
     *
     * @param editable editable text that you can work on
     * @param item item that was clicked
     * @return true if the action is valid and the popup can be dismissed
     */
    boolean onPopupItemClicked(@NonNull Editable editable, @NonNull T item);

    /**
     * Called when popup visibility state changes.
     *
     * @param shown true if the popup was just shown, false if it was just hidden
     */
    void onPopupVisibilityChanged(boolean shown);
}
