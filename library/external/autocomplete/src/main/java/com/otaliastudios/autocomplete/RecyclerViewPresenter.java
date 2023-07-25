package com.otaliastudios.autocomplete;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Simple {@link AutocompletePresenter} implementation that hosts a {@link RecyclerView}.
 * Supports {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT} natively.
 * The only contract is to
 *
 * - provide a {@link RecyclerView.Adapter} in {@link #instantiateAdapter()}
 * - call {@link #dispatchClick(Object)} when an object is clicked
 * - update your data during {@link #onQuery(CharSequence)}
 *
 * @param <T> your model object (the object displayed by the list)
 */
public abstract class RecyclerViewPresenter<T> extends AutocompletePresenter<T> {

    private RecyclerView recycler;
    private ClickProvider<T> clicks;
    private Observer observer;

    public RecyclerViewPresenter(@NonNull Context context) {
        super(context);
    }

    @Override
    protected final void registerClickProvider(@NonNull ClickProvider<T> provider) {
        this.clicks = provider;
    }

    @Override
    protected final void registerDataSetObserver(@NonNull DataSetObserver observer) {
        this.observer = new Observer(observer);
    }

    @NonNull
    @Override
    protected ViewGroup getView() {
        recycler = new RecyclerView(getContext());
        RecyclerView.Adapter adapter = instantiateAdapter();
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(instantiateLayoutManager());
        if (observer != null) {
            adapter.registerAdapterDataObserver(observer);
            observer = null;
        }
        return recycler;
    }

    @Override
    protected void onViewShown() {}

    @CallSuper
    @Override
    protected void onViewHidden() {
        recycler = null;
        observer = null;
    }

    @SuppressWarnings("unused")
    @Nullable
    protected final RecyclerView getRecyclerView() {
        return recycler;
    }

    /**
     * Dispatch click event to {@link AutocompleteCallback}.
     * Should be called when items are clicked.
     *
     * @param item the clicked item.
     */
    protected final void dispatchClick(@NonNull T item) {
        if (clicks != null) clicks.click(item);
    }

    /**
     * Request that the popup should recompute its dimensions based on a recent change in
     * the view being displayed.
     *
     * This is already managed internally for {@link RecyclerView} events.
     * Only use it for changes in other views that you have added to the popup,
     * and only if one of the dimensions for the popup is WRAP_CONTENT .
     */
    @SuppressWarnings("unused")
    protected final void dispatchLayoutChange() {
        if (observer != null) observer.onChanged();
    }

    /**
     * Provide an adapter for the recycler.
     * This should be a fresh instance every time this is called.
     *
     * @return a new adapter.
     */
    @NonNull
    protected abstract RecyclerView.Adapter instantiateAdapter();

    /**
     * Provides a layout manager for the recycler.
     * This should be a fresh instance every time this is called.
     * Defaults to a vertical LinearLayoutManager, which is guaranteed to work well.
     *
     * @return a new layout manager.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected RecyclerView.LayoutManager instantiateLayoutManager() {
        return new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    }

    private final static class Observer extends RecyclerView.AdapterDataObserver {

        private DataSetObserver root;

        Observer(@NonNull DataSetObserver root) {
            this.root = root;
        }

        @Override
        public void onChanged() {
            root.onChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            root.onChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            root.onChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            root.onChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            root.onChanged();
        }
    }
}
