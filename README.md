# Kotlin Coroutines

What is a coroutine? To put it simply, a coroutine is a way creating a suspendable code that can run
concurrently with code running on other threads. But what does that mean? Let's take a deeper look
and see what coroutines have to offer us, what they are not, and where/when we would want to use
them in the scope of an android project. First, let's define a couple things so that we're all on
the same page.

> **Thread** - A thread is a [sequential line of execution] in a program. The Java Virtual Machine
> allows an application to have multiple threads of execution running concurrently.
> [Source](https://developer.android.com/reference/java/lang/Thread)

> **Concurrency** - The coordination and management of independent lines of execution. These
> executions can be truly parallel or simply be managed by interleaving. They can communicate via
> shared memory or message passing.
> [Source](https://cs.lmu.edu/~ray/notes/introconcurrency/)

> **Multithreading** - Execution of a program with multiple threads.
> [Source](https://cs.lmu.edu/~ray/notes/introconcurrency/)

> **Main-safe** - We consider a function main-safe when it doesn't block UI updates on the main thread.
> [Source](https://developer.android.com/kotlin/coroutines#use-coroutines-for-main-safety)

> **Suspend Function** - A function that could be started, paused, and resume.
> [Source](https://www.geeksforgeeks.org/suspend-function-in-kotlin-coroutines/)

## Coroutine Scopes

In the context of Android, we have 3 possible scopes for coroutines, each with their own specific
behavior. That is global, lifecycle and viewmodel.

### Global Scope

A coroutine launched in the global scope will live as long as the application. That makes it the
least desirable scope in which to launch most coroutines. Launching a coroutine in the global
routine, you can expect the following behavior:

- If a coroutine finishes before the application is shut down, the coroutine will be destroyed and
  not live for the remainder of the applications life. (This is good.)
- A coroutine launched in the global scope will continue to run through all lifecycle and activity
  changes. This means activity/fragment lifecycle changes will not cancel the coroutine. This isn't
  always desirable behavior for our app. Example: A coroutine that adds 1 to a counter every second
  will continue to count as the user rotates the screen and navigates through activities and
  fragments.

While there may be use cases for using global scope, this shouldn't be your default choice and
should only be used when necessary. You can gain access to using the global scope coroutine context
with the following dependency in your modules build gradle:

```groovy
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2'
```

### Lifecycle Scope

A coroutine in the lifecycle scope, is bound to the lifecycle of the activity. So say we have a
coroutine function that adds 1 to a counter every second. This counter will be reset if the user
rotates the screen and onDestroy gets called. This coroutine will also be canceled when navigate to
a new activity.

We can gain access to the lifecycle scope in our projects by adding the following dependency:

```groovy
implementation 'androidx.activity:activity-ktx:1.4.0'
```

### ViewModel Scope

When we use fragments, compose and in single activity apps having our coroutines last for the entire
life of the activity becomes undesirable, for the same reason global scope is undesirable. In that
case we can use the viewmodel scope. The viewmodel scope is effective because now our coroutines
will only last as long as our viewmodel is alive. This means we can set up viewmodel's for each
fragment or composable function that we want to limit our coroutines too. So if our composable
functions and fragments that our viewmodel's are attached to die, so will our coroutines.

If we want to use viewmodel scoped coroutines in our project, we can add the following dependency:

```groovy
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0'
```

## Practical Examples

So now that we understand a little more about what's involved with coroutines and the various scopes
in which we can run them in, let's play around with them a little so we can get a practical
understanding of the can work in our application. For this post, I'm going to use lifecycleScope in
the main activity of a new project, since we won't be using any viewmodel's today.

Let's start with a simple example:

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(
            TAG, "Pre lifecycleScope - onCreate, running on thread: ${
                Thread.currentThread().name
            }"
        )

        lifecycleScope.launch {
            Log.d(TAG, "LifecycleScope, running on thread: ${Thread.currentThread().name}")
        }

        Log.d(
            TAG, "Post lifecycleScope - onCreate running on thread: ${
                Thread.currentThread().name
            }"
        )
    }

    companion object {
        const val TAG = "KotlinCoroutines"
    }
}
```

If we run our app, we'll see that our logs print out

```
Pre lifecycleScope - onCreate, running on thread: main
LifecycleScope, running on thread: main
Post lifecycleScope - onCreate running on thread: main
```

This is to be expected. Our code has run synchronously. But coroutines shine in areas where code
takes some time to run. We can easily make our lifecycleScope.launch take more time by simply adding
a delay, which is a suspended function and can only be called from a coroutine context or another
suspended function. So now our lifecycleScope.launch should look something like this:

```kotlin
lifecycleScope.launch {
    delay(3000L)
    Log.d(TAG, "LifecycleScope, running on thread: ${Thread.currentThread().name}")
}
```

Now when we run our logs we will see:

```
Pre lifecycleScope - onCreate, running on thread: main
Post lifecycleScope - onCreate running on thread: main
```

And three seconds later we will see `LifecycleScope, running on thread: main`
pop up in our logs.

What this means is that even through our lifecycle scope is running on the main thread, it did not
block the main thread at all. So our code here can be considered main-safe. To further illustrate
main-safety let's update the UI after the lifecycle scope block and see if the UI does indeed update
before our delayed lifecycle scope is done running. (I have given the default textview in my
activity_main.xml file an id of `tv_dummy`.)

```kotlin
val dummyTextView = findViewById<TextView>(R.id.tv_dummy)
Log.d(
    TAG, "Pre lifecycleScope - onCreate, running on thread: ${
        Thread.currentThread()
            .name
    }"
)
lifecycleScope.launch {
    delay(3000L)
    Log.d(TAG, "LifecycleScope, running on thread: ${Thread.currentThread().name}")
}
dummyTextView.text = "Hello, from after lifecycle scope!"
```

When we run this code, we will see that our UI immediately has the new text displayed in the
textview, so our code is main-safe. But what if we want to make this code not main-safe? We can use
something called run blocking. Run blocking gives us a coroutine scope that blocks the main thread
until the code has been completed. So if we change our lifecycleScope.launch to a runBlocking scope:

```kotlin
val dummyTextView = findViewById<TextView>(R.id.tv_dummy)
Log.d(
    TAG, "Pre lifecycleScope - onCreate, running on thread: ${
        Thread.currentThread()
            .name
    }"
)
runBlocking {
    delay(3000L)
    Log.d(TAG, "LifecycleScope, running on thread: ${Thread.currentThread().name}")
}
dummyTextView.text = "Hello, from after lifecycle scope!"
```

We will observe that the UI is blank for 3 seconds before displaying the UI. That is because our
runBlocking scope has stopped the UI from updating for 3 seconds until it lets our onCreate
lifecycle finish. Our code is no longer main safe.

### Running Multiple Coroutines and Suspended Functions

To explore a little further, let's see what happens when we try to launch more than one suspended
function and coroutine at once. Let's start with just a suspended function first.

```kotlin
suspend fun greet() {
    delay(3000L)
    Log.d(TAG, "Greetings from thread: ${Thread.currentThread().name}")
}
```

Now we call this function from inside our lifecycle scope twice and see what happens.

```kotlin
Log.d(
    TAG, "Pre lifecycleScope - onCreate, running on thread: ${
        Thread.currentThread()
            .name
    }"
)
lifecycleScope.launch {
    greet()
    greet()
}
Log.d(
    TAG, "Post lifecycleScope - onCreate running on thread: ${
        Thread.currentThread()
            .name
    }"
)
```

Running this code logs `Pre lifecycleScope - onCreate, running on thread: main`
and `Post lifecycleScope - onCreate running on thread: main` immediately when the application gets
launched. Then, after 3 seconds `Greetings from thread: main` gets ran, followed by
`Greetings from thread: main` again after another subsequent 3 seconds. This seems to make sense.
Lifecycle scope is running both functions is sequence, each taking 3 seconds, making the full
lifecycle scope launch take a total of 6 seconds to complete. But what happens if we add a lifecycle
scope for the main thread inside the suspended function? Something like this:

```kotlin
suspend fun greet() {
    lifecycleScope.launch {
        delay(3000L)
        Log.d(TAG, "Greetings from thread: ${Thread.currentThread().name}")
    }
}
```

If we do this and run our code we will see our pre and post lifecycleScope logs logging right away
when we start the app. However, 3 seconds later both greet functions will print at the same time,
from the main thread. This is because even though they are running on the same main thread, the
functions are able to be suspended an ran concurrently alongside each other since they have they are
launched in their own lifecycle scope. This is pretty nifty. So, for example if we had 5 network
requests that we wanted to run at the same time, we could launch them each in their own lifecycle
scope to have them all fire off at approximately the same time without slowing anything down. Even
so, we still don't want to clog up our main thread with a huge amount of long running and heavy
computational tasks, because that could still slow down and clog up the main thread. Which leads us
to our next topic, dispatchers.

## Dispatchers

To divide up the workload, we can pass the context of the thread we want our code to run on with the
use of dispatchers. There are 4 types of dispatchers that we can pass as context for our coroutine
launch scopes.

- Main: Used for UI operations and quick work such as calling suspend functions.
- IO: Great for running network, database or disc operations. Think Input/Output.
- Default: Best used for CPU-intensive work, like sorting or parsing giant lists of data.
- Unconfined: This dispatcher isn't confined to any thread. Instead it executes the coroutine in the
  current scopes context and let's the coroutine resume in whatever thread that is used by it's
  suspended function and does not dictate which thread it needs to run on.

When creating our coroutine scope launch, we can specify which thread we want it to launch, as well
as using `withContext` to switch contexts of the thread your running code on in a current coroutine
scope. Let's check out some examples below to see how this works.

```kotlin
Log.d(
    TAG, "Pre lifecycleScope - onCreate, running on thread: ${
        Thread.currentThread()
            .name
    }"
)
lifecycleScope.launch(Dispatchers.Main) {
    Log.d(
        TAG, "LifecycleScope, context MAIN - running on thread: ${
            Thread.currentThread()
                .name
        }"
    )
    withContext(Dispatchers.IO) {
        Log.d(
            TAG, "LifecycleScope, context IO - running on thread: ${
                Thread.currentThread()
                    .name
            }"
        )
    }
}
Log.d(
    TAG, "Post lifecycleScope - onCreate running on thread: ${
        Thread.currentThread()
            .name
    }"
)
```

Will result in the following logs:

```
Pre lifecycleScope - onCreate, running on thread: main
Post lifecycleScope - onCreate running on thread: main
LifecycleScope, context MAIN - running on thread: main
LifecycleScope, context IO - running on thread: DefaultDispatcher-worker-2
```

So we can see here that we choose `Dispatchers.Main` in our lifecycle scope, everything is the same,
but once we switch the context to IO we can see our code running on a separate thread.

## Recapping

So today we've learned about:

- Key definitions to understand coroutines.
- Different coroutine scopes in Android.
- Practical examples about the behavior of coroutines.
- Coroutine dispatchers and running code on different threads.

There is obviously a lot more to learn about coroutines, from jobs to async await and edge case
behavior, but we can learn more about that in a later post. What areas of coroutines would you like
to explore together next? Leave a comment below with your thoughts, constructive feedback, or any
corrections to explanations/examples. Thanks for reading, happy coding!
