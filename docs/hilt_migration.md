Useful links:
- https://dagger.dev/hilt/migration-guide
- https://dagger.dev/hilt/quick-start

Hilt is built on top of Dagger 2 and simplify usage by removing needs to create components manually.

When you create a new feature, you should have the following:

Annotate your Activity with @AndroidEntryPoint
If you have a BottomSheetFragment => Annotate it with @AndroidEntryPoint
Otherwise => Add your Fragment to the FragmentModule
Add your ViewModel.Factory to the MavericksViewModelModule
Makes sure your ViewModel as the following code:

```
 @AssistedFactory
    interface Factory: MavericksAssistedViewModelFactory<MyViewModel, MyViewState> {
        override fun create(initialState: MyViewState): MyViewModel
    }

    companion object : MavericksViewModelFactory<MyViewModel, MyViewState> by hiltMavericksViewModelFactory()
```

## Some remarks

@MavericksViewModelScope dependencies can't be injected inside Fragments/Activities
You can only inject @Singleton, @MavericksViewModelScope or unscoped dependencies inside Maverick ViewModels
You can access some specific dependencies from Singleton component by using
```
context.singletonEntryPoint()
```
Be aware that only the app has been migrated to Hilt and not the SDK.

