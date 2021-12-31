Useful links:
- https://airbnb.io/mavericks/#/new-2x

Mavericks 2 is replacing MvRx, by removing usage of Rx by Flow, both internally and in the API.
See the link ^ to have more intel, but basically, the changes are:

session.rx() => session.flow()
room.rx() => room.flow()
subscribe { }.disposeOnClear() => onEach { }.launchIn(viewModelScope)

Only using manually onEach requires to add launchIn,any other methods provided by Mavericks on viewModel and activity/fragment are already taking care of lifecycle.