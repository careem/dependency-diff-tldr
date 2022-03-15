This removes an "extra" dependency that is already pulled in by another one.
The result should be empty.

i.e. in removes:

```
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0'

```

in a project that already imports:

```
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.0'
```
