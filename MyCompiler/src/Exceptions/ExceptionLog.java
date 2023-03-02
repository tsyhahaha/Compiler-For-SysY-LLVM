package Exceptions;

import java.util.*;

public class ExceptionLog {
    private static ExceptionLog instance = new ExceptionLog();
    private final List<MyException> exceptionList = new ArrayList<>();
    private static MyException lastException = null;

    public static ExceptionLog getInstance() {
        if(instance == null) {
            instance = new ExceptionLog();
        }
        return instance;
    }

    public static void addException(MyException e) {
        if(lastException == null || !lastException.equals(e)) {
            lastException = e;
            System.err.println("add exception: " + e);
            instance.exceptionList.add(e);
        }
    }

    public String toString() {
        this.exceptionList.sort((e1, e2) -> {
            Integer line1 = e1.getLine();
            Integer line2 = e2.getLine();
               return line1.compareTo(line2);
        });
        StringBuilder sb = new StringBuilder();
        for(Exception e: exceptionList) {
            sb.append(e.toString()).append("\n");
        }
        if(sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
}
