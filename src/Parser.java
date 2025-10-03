import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    public static void main(String[] args) {
        String path;
        if (args.length > 0) {
            path = args[0];
        } else {
            path = "access.log";
        }

        analyzeFile(path);
    }
    public static void analyzeFile(String path) {
        File file = new File(path);
        System.out.println("Проверка лога: " + file.getAbsolutePath());

        Statistics statistics = new Statistics();

        try (
                FileReader fileReader = new FileReader(file);
                BufferedReader reader = new BufferedReader(fileReader)
        ) {
            int totalLines = 0;
            String line;

            System.out.println("Обработка файла...");

            while ((line = reader.readLine()) != null) {
                int length = line.length();
                totalLines++;

                if (length > 1024) {
                    throw new LineTooLongException(
                            "Строка #" + totalLines + " превышает 1024 символа. Длина: " + length
                    );
                }

                // объект LogEntry и добавляем в статистику
                LogEntry logEntry = new LogEntry(line);
                statistics.addEntry(logEntry);

                if (totalLines % 1000 == 0) {
                    System.out.println("Обработано строк: " + totalLines);
                }
            }
            if (totalLines == 0) {
                System.out.println("Файл пуст.");
            } else {
                System.out.println("\nРезультаты анализа:");
                System.out.println("Всего строк: " + totalLines);
                System.out.printf("Средний объем трафика за час: %.2f байт/час\n", statistics.getTrafficRate());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
// Enum для HTTP запросов
enum HttpMethod {
    GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH, UNKNOWN
}
// Класс строк лог-файла
class LogEntry {
    private final String ipAddress;
    private final LocalDateTime dateTime;
    private final HttpMethod method;
    private final String path;
    private final int responseCode;
    private final int dataSize;
    private final String referer;
    private final UserAgent userAgent;

    public LogEntry(String logLine) {
        // выражение для парсинга строки лога
        Pattern pattern = Pattern.compile("^(\\S+) - - \\[(.+?)\\] \"(\\S+) (\\S+) HTTP/\\d\\.\\d\" (\\d+) (\\d+) \"([^\"]*)\" \"([^\"]*)\"$");
        Matcher matcher = pattern.matcher(logLine);

        if (matcher.find()) {
            this.ipAddress = matcher.group(1);
            this.dateTime = parseDateTime(matcher.group(2));
            this.method = parseHttpMethod(matcher.group(3));
            this.path = matcher.group(4);
            this.responseCode = Integer.parseInt(matcher.group(5));
            this.dataSize = Integer.parseInt(matcher.group(6));
            this.referer = matcher.group(7).equals("-") ? null : matcher.group(7);
            this.userAgent = new UserAgent(matcher.group(8));
        } else {
            throw new IllegalArgumentException("Неверный формат строки лога: " + logLine);
        }
    }
    private LocalDateTime parseDateTime(String dateTimeStr) {
        // формат для даты
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        return LocalDateTime.parse(dateTimeStr, formatter);
    }

    private HttpMethod parseHttpMethod(String methodStr) {
        try {
            return HttpMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            return HttpMethod.UNKNOWN;
        }
    }
    // геттеры
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getDateTime() { return dateTime; }
    public HttpMethod getMethod() { return method; }
    public String getPath() { return path; }
    public int getResponseCode() { return responseCode; }
    public int getDataSize() { return dataSize; }
    public String getReferer() { return referer; }
    public UserAgent getUserAgent() { return userAgent; }
}
// Класс User-Agent
class UserAgent {
    private final String osType;
    private final String browser;

    public UserAgent(String userAgentString) {
        this.osType = detectOsType(userAgentString);
        this.browser = detectBrowser(userAgentString);
    }
    private String detectOsType(String userAgent) {
        if (userAgent.toLowerCase().contains("windows")) {
            return "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            return "macOS";
        } else if (userAgent.toLowerCase().contains("linux")) {
            return "Linux";
        } else if (userAgent.toLowerCase().contains("android")) {
            return "Android";
        } else if (userAgent.toLowerCase().contains("ios") || userAgent.toLowerCase().contains("iphone")) {
            return "iOS";
        } else {
            return "Other";
        }
    }
    private String detectBrowser(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("edge")) {
            return "Edge";
        } else if (ua.contains("firefox")) {
            return "Firefox";
        } else if (ua.contains("chrome") && !ua.contains("chromium")) {
            return "Chrome";
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        } else if (ua.contains("opera") || ua.contains("opr/")) {
            return "Opera";
        } else {
            return "Other";
        }
    }
    // Геттеры
    public String getOsType() { return osType; }
    public String getBrowser() { return browser; }
}
// Класс статистики
class Statistics {
    private int totalTraffic;
    private LocalDateTime minTime;
    private LocalDateTime maxTime;

    public Statistics() {
        this.totalTraffic = 0;
        this.minTime = null;
        this.maxTime = null;
    }
    public void addEntry(LogEntry entry) {
        // Добавляем трафик
        this.totalTraffic += entry.getDataSize();

        // минимальное максимальное время
        LocalDateTime entryTime = entry.getDateTime();
        if (minTime == null || entryTime.isBefore(minTime)) {
            minTime = entryTime;
        }
        if (maxTime == null || entryTime.isAfter(maxTime)) {
            maxTime = entryTime;
        }
    }
    public double getTrafficRate() {
        if (minTime == null || maxTime == null || minTime.equals(maxTime)) {
            return 0.0;
        }
        // опрееление разницы в часах
        long hoursBetween = ChronoUnit.HOURS.between(minTime, maxTime);
        if (hoursBetween == 0) {
            hoursBetween = 1; // Минимум час
        }
        return (double) totalTraffic / hoursBetween;
    }
    // Геттеры
    public int getTotalTraffic() { return totalTraffic; }
    public LocalDateTime getMinTime() { return minTime; }
    public LocalDateTime getMaxTime() { return maxTime; }
}
