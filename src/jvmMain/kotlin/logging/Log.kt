package logging

val log = Log()
class Log {
    fun debug(tag: String, message: String) {
        println("$tag: $message")
    }
}