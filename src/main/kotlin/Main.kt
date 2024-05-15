import kotlinx.coroutines.*
import java.lang.Exception
import java.lang.IllegalStateException
import kotlin.random.Random
import kotlin.system.measureTimeMillis

// 1
suspend fun helloCoroutine() = coroutineScope {
    launch {
        println("Coroutine")
    }
    print("Hello ")
}

// 2
suspend fun coroutineScopeBuilder() = coroutineScope {
    // 2.1 CoroutineContext 정보
    println(this)
    println(this.coroutineContext)

    // 2.2 현재 coroutine이 실행되는 Thread
    println("2.2 Running on : ${Thread.currentThread().name}")

    // 2.2 launch라는 coroutine Builder
    // coroutine Builder는 반드시 Coroutine Scope에서 정의
    // coroutine의 시작은 Coroutine Scope
    launch {
        println("launch1: ${Thread.currentThread().name}")
        println("1")
    }

    /**
     *  launch는 Job이라는 객체를 return
     *  fire and forgot 방식
     */
    val job = launch {
        println("launch2: ${Thread.currentThread().name}")
        // susend point 중단 가능한 함수.
        // 자신이 사용하던 Thread를 다른 coroutine혹은 로직이 사용하도록 양보해줌
        // Thread.sleep으로 하면, 양보하지 않음, 1초 만큼 기다리고 자신이 다시 수행
        delay(1000L)
//        Thread.sleep(1000L)
        println("2")
    }
    // return한 Job을 가지고, 제어를 할 수 있음
    // join은 해당 job이 수행 완료될 때까지 대기
    job.join()

    launch {
        println("launch3: ${Thread.currentThread().name}")
        println("3")
    }

    println("4")
    //상위 Coroutine은 자식 Coroutine들이 모두 완료되기 전까지 대기
}

private suspend fun doOne() {
    try {
        println("doOne(): ${Thread.currentThread().name}")
        delay(800L)
        println("1")
    } finally {
        println("doOne() is canceled")
    }
}

private suspend fun doTwo() {
    try {
        println("doTwo(): ${Thread.currentThread().name}")
        // susend point 중단 가능한 함수.
        // 자신이 사용하던 Thread를 다른 coroutine혹은 로직이 사용하도록 양보해줌
        // Thread.sleep으로 하면, 양보하지 않음, 1초 만큼 기다리고 자신이 다시 수행
        delay(1000L)
//        Thread.sleep(1000L)
        println("2")
    } finally {
        println("doTwo() is Canceled")
    }
}

private fun doThree() {
    println("doThree(): ${Thread.currentThread().name}")
    println("3")
}

suspend fun doOneTwoThree() = coroutineScope {
    // 위 2번 예제에서 코드를 분리해 봅시다.
    launch { doOne() }
    launch { doTwo() }
    launch { doThree() }

    println("4")
}

suspend fun getRandomOne(): Int {
    delay(1000L)
    return Random.nextInt(0, 10)
}

suspend fun getRandomTwo(): Int {
    delay(1500L)
    return Random.nextInt(0, 10)
}

// 아래 함수에서는 하나의 CoroutineScope에서 getRandomOne(), getRandonTwo()가 순차 실행 됨
suspend fun getRandomNumber() = coroutineScope {
    /**
     *  4.1 하나의 CoroutineScope에서 각각 suspend 함수를 호출.
     */
    val elapsedTime1 = measureTimeMillis {
        val value1 = getRandomOne()
        val value2 = getRandomTwo()
        println("${value1} + ${value2} = ${value1 + value2}")
    }

    println("4.1 elapsed time : $elapsedTime1")

    /**
     *  async{} 라는 또다른 coroutine Builder를 사용
     *
     *  async{}는 수행의 결과를 return 받아야 할 때 사용.
     *  launch{}를 사용해도 동시성을 만족.
     */
    val elapsedTime2 = measureTimeMillis {
        val value1 = async { getRandomOne() }
        val value2 = async { getRandomTwo() }
        println("${value1.await()} + ${value2.await()} = ${value1.await() + value2.await()}")
    }

    println("4.2 elapsed time : $elapsedTime2")

    /**
     *  중요!
     *  async를 사용해서 동시성을 만족시키는 것이 아니라,
     *  각각의 로직을 별도의 자식 coroutineScope에서 실행시켜, 순차 실행이 아닌, 협력적으로 실행되도록 하는 것이 중요!
     */
}

suspend fun getRandomOneTry(): Int {
    try {
        delay(1000L)
        return Random.nextInt(0, 10)
    } finally {
        println("getRandomOne is Canceled..")
    }
}

suspend fun getRandomTwoException(): Int {
    delay(1500L)
    throw IllegalStateException()
}

suspend fun doOneTwoThreeCancellation() = coroutineScope {
    val job1 = launch { doOne() }
    val job2 = launch { doTwo() }
    val job3 = launch { doThree() }

    delay(500L)

    job1.cancel()
    job2.cancel()
    job3.cancel()

    println("4")
}

suspend fun getRandomNumberWithCancellation() = coroutineScope {
    val value1 = async { getRandomOneTry() }
    val value2 = async { getRandomTwoException() }

    try {
        println("${value1.await()} + ${value2.await()} = ${value1.await() + value2.await()}")
    } finally {
        println("getRandomNumberWithCancellation is canceled")
    }
}

/**
 *  Coroutine Dispathcer
 *
 *  cooutine Builder를 통해 Coroutine을 생성할 때,
 *  어떤 Thread에서 로직을 수행 할지, Thread를 정할 수 있다.
 *  - Default
 *  - IO
 *  - Main
 */
val scope = CoroutineScope(Dispatchers.Default)
suspend fun printRandom() = coroutineScope {
    val elapsedTime1 = measureTimeMillis {
        val value1 = async(Dispatchers.Default) {
            println("${Thread.currentThread().name}")
            getRandomOne()
        }
        val value2 = async(Dispatchers.IO) {
            println("${Thread.currentThread().name}")
            getRandomTwo()
        }
        println("${value1.await()} + ${value2.await()} = ${value1.await() + value2.await()}")
    }
    println("6. elapsed time : $elapsedTime1")
}


fun main(): Unit = runBlocking {

    /**
     *  1. 가장 간단한 coroutine
     *  수행 순서가 중요. Hello Coroutine
     */
    println("1. ==============")
    helloCoroutine()


    /**
     *  2. Coroutine Scope / Coroutine Builder / Scope Builder
     *
     *  coroutine Builder는 Coroutine Scope내에서만 호출 가능
     *  - launch, async
     *
     *  scope builder
     *  - coroutineScope {}
     */
    println("2. ==============")
    coroutineScopeBuilder()


    /**
     *  3. suspend function
     *
     *  중단 가능한 함수. suspend point가 됨
     *  중단 가능한 함수의 의미는 다른 로직들과 협력적으로 수행하겠다는 의미
     *  suspend function은 CoroutineScope에서 호출 혹은 다른 Suspend Function에서만 호출 가능
     */
    println("3. ==============")
    doOneTwoThree()


    /**
     *  4. suspend function을 활용
     *   - susepdn function을 사용해서, 구조회된 동시성을 만족 시킬 수 있음
     */
    println("4. ==============")
    getRandomNumber()


    /**
     *  5. 구조화된 동시성 2
     *
     *  Coroutine은 가족관계증명서
     *  - 자식 Coroutine 중 한개라도 예외가 발생되어, 실행이 Cancel되면
     *    취소 이벤트라 부모, 자식, 형제 들에게 전파되어, 모든 Coroutine이 취소
     */
    println("5. ==============")
    doOneTwoThreeCancellation()
    try {
        getRandomNumberWithCancellation()
    } catch (e: Exception) {
        println("main() :: $e")
    }

    println("6. ==============")
    printRandom()
}