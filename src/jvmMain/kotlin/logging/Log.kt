package logging

const val log = Log()
class Log {
    fun debug(tag: String, message: String) {
        println("$tag: $message")
    }
}