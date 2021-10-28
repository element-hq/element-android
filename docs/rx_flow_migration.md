Useful links:
- https://github.com/ReactiveCircus/FlowBinding
- https://ivanisidrowu.github.io/kotlin/2020/08/09/Kotlin-Flow-Migration-And-Testing.html


Rx is now completely removed from Element dependencies.
Some examples of the changes:

```
         sharedActionViewModel
                .observe()
                .subscribe { handleQuickActions(it) }
                .disposeOnDestroyView()
 ```               
     
became 
           
 ```              
         sharedActionViewModel 
                .stream()
                .onEach { handleQuickActions(it) }
                .launchIn(viewLifecycleOwner.lifecycleScope)

```

Inside fragment use  
```
launchIn(viewLifecycleOwner.lifecycleScope)
```
Inside activity use
```
launchIn(lifecycleScope)
```
Inside viewModel use
```
launchIn(viewModelScope)
```

Also be aware that when using these scopes the coroutine is launched on Dispatchers.Main by default.


