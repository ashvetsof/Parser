import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OS {
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

                // Создаем объект LogEntry и добавляем в статистику
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

                // Выводим статистику по браузерам и ОС
                System.out.println("\nСтатистика по браузерам:");
                statistics.printBrowserStatistics();

                System.out.println("\nСтатистика по ОС:");
                statistics.printOsStatistics();

                // Дополнительная статистика
                System.out.println("\nСуществующие страницы сайта:");
                Set<String> existingPages = statistics.getExistingPages();
                for (String page : existingPages) {
                    System.out.println("  " + page);
                }

                System.out.println("\nСтатистика ОС (доли):");
                Map<String, Double> osStats = statistics.getOsStatistics();
                for (Map.Entry<String, Double> entry : osStats.entrySet()) {
                    System.out.printf("  %s: %.2f%%\n", entry.getKey(), entry.getValue() * 100);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

// Класс исключения для слишком длинных строк
class LineTooLongException extends RuntimeException {
    public LineTooLongException(String message) {
        super(message);
    }
}

// Enum для методов HTTP-запросов
enum HttpMethod {
    GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH, UNKNOWN
}

// Класс для представления строки лог-файла
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
        // Регулярное выражение для парсинга строки лога
        Pattern pattern;
        pattern = Pattern.compile("^(\\S+) - - \\[(.+?)\\] \"(\\S+) (\\S+) HTTP/\\d\\.\\d\" (\\d+) (\\d+) \"([^\"]*)\" \"([^\"]*)\"$");
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
        try {
            // Исправленный формат для даты типа "25/Sep/2022:06:25:04 +0300"
            // Убираем временную зону для упрощения парсинга
            String cleanedDateTime = dateTimeStr.split(" ")[0];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss", Locale.ENGLISH);
            return LocalDateTime.parse(cleanedDateTime, formatter);
        } catch (Exception e) {
            System.err.println("Ошибка парсинга даты: " + dateTimeStr);
            return LocalDateTime.now();
        }
    }

    private HttpMethod parseHttpMethod(String methodStr) {
        try {
            return HttpMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            return HttpMethod.UNKNOWN;
        }
    }

    // Геттеры
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getDateTime() { return dateTime; }
    public HttpMethod getMethod() { return method; }
    public String getPath() { return path; }
    public int getResponseCode() { return responseCode; }
    public int getDataSize() { return dataSize; }
    public String getReferer() { return referer; }
    public UserAgent getUserAgent() { return userAgent; }
}

// Класс для представления User-Agent
class UserAgent {
    private final String osType;
    private final String browser;

    public UserAgent(String userAgentString) {
        this.osType = detectOsType(userAgentString);
        this.browser = detectBrowser(userAgentString);
    }

    private String detectOsType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) {
            return "Windows";
        } else if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return "macOS";
        } else if (ua.contains("linux")) {
            return "Linux";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        } else {
            return "Other";
        }
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) {
            return "Edge";
        } else if (ua.contains("firefox")) {
            return "Firefox";
        } else if (ua.contains("chrome")) {
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

// Класс для статистики
class Statistics {
    private int totalTraffic;
    private LocalDateTime minTime;
    private LocalDateTime maxTime;
    private Map<String, Integer> browserStats;
    private Map<String, Integer> osStats;

    // Новые поля для хранения существующих страниц и статистики ОС
    private Set<String> existingPages;
    private Map<String, Integer> osFrequency;

    public Statistics() {
        this.totalTraffic = 0;
        this.minTime = null;
        this.maxTime = null;
        this.browserStats = new HashMap<String, Integer>();
        this.osStats = new HashMap<String, Integer>();
        this.existingPages = new HashSet<String>();
        this.osFrequency = new HashMap<String, Integer>();
    }

    public void addEntry(LogEntry entry) {
        // Добавляем трафик
        this.totalTraffic += entry.getDataSize();

        // Обновляем minTime и maxTime
        LocalDateTime entryTime = entry.getDateTime();
        if (minTime == null || entryTime.isBefore(minTime)) {
            minTime = entryTime;
        }
        if (maxTime == null || entryTime.isAfter(maxTime)) {
            maxTime = entryTime;
        }

        // Собираем статистику по браузерам и ОС
        UserAgent userAgent = entry.getUserAgent();
        String browser = userAgent.getBrowser();
        String os = userAgent.getOsType();

        browserStats.put(browser, browserStats.getOrDefault(browser, 0) + 1);
        osStats.put(os, osStats.getOrDefault(os, 0) + 1);

        // Добавляем существующие страницы (код ответа 200)
        if (entry.getResponseCode() == 200) {
            existingPages.add(entry.getPath());
        }

        // Собираем частоту операционных систем
        osFrequency.put(os, osFrequency.getOrDefault(os, 0) + 1);
    }

    /**
     * Возвращает список всех существующих страниц сайта (с кодом ответа 200)
     * @return Set<String> содержащий адреса существующих страниц
     */
    public Set<String> getExistingPages() {
        return new HashSet<String>(existingPages);
    }

    /**
     * Возвращает статистику операционных систем в виде долей (от 0 до 1)
     * @return Map<String, Double> где ключ - название ОС, значение - доля от общего количества
     */
    public Map<String, Double> getOsStatistics() {
        Map<String, Double> osProportions = new HashMap<String, Double>();

        // Вычисляем общее количество записей
        int totalRequests = 0;
        for (int count : osFrequency.values()) {
            totalRequests += count;
        }

        // Рассчитываем долю для каждой операционной системы
        for (Map.Entry<String, Integer> entry : osFrequency.entrySet()) {
            String os = entry.getKey();
            int count = entry.getValue();
            double proportion = (double) count / totalRequests;
            osProportions.put(os, proportion);
        }

        return osProportions;
    }

    public double getTrafficRate() {
        if (minTime == null || maxTime == null || minTime.equals(maxTime)) {
            return 0.0;
        }

        long hoursBetween = ChronoUnit.HOURS.between(minTime, maxTime);
        if (hoursBetween == 0) {
            hoursBetween = 1;
        }

        return (double) totalTraffic / hoursBetween;
    }

    public void printBrowserStatistics() {
        for (Map.Entry<String, Integer> entry : browserStats.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " запросов");
        }
    }

    public void printOsStatistics() {
        for (Map.Entry<String, Integer> entry : osStats.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " запросов");
        }
    }

    // Геттеры
    public int getTotalTraffic() { return totalTraffic; }
    public LocalDateTime getMinTime() { return minTime; }
    public LocalDateTime getMaxTime() { return maxTime; }
    public Map<String, Integer> getBrowserStats() { return browserStats; }
    public Map<String, Integer> getOsStats() { return osStats; }
}