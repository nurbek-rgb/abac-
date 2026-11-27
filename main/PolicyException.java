package kz.abac;

/** Саясат файлы қате болғанда (жарамсыз JSON, белгісіз оператор т.б.) лақтырылады. */
public class PolicyException extends RuntimeException {
    public PolicyException(String message) {
        super(message);
    }

    public PolicyException(String message, Throwable cause) {
        super(message, cause);
    }
}

