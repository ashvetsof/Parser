import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OS {
    public static void main(String[] args) {
        String inputPath;
        String outputPath;
        if (args.length > 0) {
            inputPath = args[0];
        } else {
            inputPath = "access.log";
        }
        if (args.length > 1) {
            outputPath = args[1];
        } else {
            outputPath = "analysis_result.txt";
        }
        analyzeFile(inputPath, outputPath);
    }

    public static void analyzeFile(String inputPath, String outputPath) {
        File inputFile = new File(inputPath);
        System.out.println("Проверка лога: " + inputFile.getAbsolutePath());
        System.out.println("Результат будет сохранен в: " + outputPath);

        Statistics stats = new Statistics();
        try (
                FileReader fileReader = new FileReader(inputFile);
                BufferedReader reader = new BufferedReader(fileReader);
                PrintWriter writer = new PrintWriter(new FileWriter(outputPath))
        )
        {
            int totalLines = 0;
            String line;
            writer.println("Анализ лога: " + inputPath);
            writer.println("=" .repeat(50));

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
                stats.addEntry(logEntry);

                if (totalLines % 1000 == 0) {
                    System.out.println("Обработано строк: " + totalLines);
                }
            }
            if (totalLines == 0) {
                writer.println("Файл пуст.");
            } else {
                writer.println("\nРезультаты анализа:");
                writer.println("Всего строк: " + totalLines);
                writer.printf("Средний объем трафика за час: %.2f байт/час\n", stats.getTrafficRate());

                // Новые метрики
                writer.printf("Среднее количество посещений сайта за час: %.2f\n", stats.getAverageVisitsPerHour());
                writer.printf("Среднее количество ошибочных запросов в час: %.2f\n", stats.getAverageErrorRequestsPerHour());
                writer.printf("Средняя посещаемость одним пользователем: %.2f\n", stats.getAverageVisitsPerUser());

                // Новые методы
                writer.printf("Пиковая посещаемость сайта: %d посещений/секунду\n", stats.getPeakVisitsPerSecond());
                writer.printf("Максимальная посещаемость одним пользователем: %d посещений\n", stats.getMaxVisitsPerUser());

                // Список сайтов-рефереров
                Set<String> refererDomains = stats.getRefererDomains();
                writer.println("\nСайты, со страниц которых есть ссылки на текущий сайт (" + refererDomains.size() + "):");
                for (String domain : refererDomains) {
                    writer.println("  " + domain);
                }

                // статистик по браузерам и ОС
                writer.println("\nСтатистика по браузерам:");
                printBrowserStatisticsToFile(stats, writer);
                writer.println("\nСтатистика по операционным системам:");
                printOsStatisticsToFile(stats, writer);

                // список существующих страниц
                Set<String> existingPages = stats.getExistingPages();
                writer.println("\nСуществующие страницы сайта (" + existingPages.size() + "):");
                for (String page : existingPages) {
                    writer.println("  " + page);
                }

                // список несуществующих страниц
                Set<String> notFoundPages = stats.getNotFoundPages();
                writer.println("\nНесуществующие страницы сайта (" + notFoundPages.size() + "):");
                for (String page : notFoundPages) {
                    writer.println("  " + page);
                }

                // статистика ОС
                Map<String, Double> osStatistics = stats.getOsStatistics();
                writer.println("\nСтатистика операционных систем (доли):");
                for (Map.Entry<String, Double> entry : osStatistics.entrySet()) {
                    writer.printf("  %s: %.2f%%\n", entry.getKey(), entry.getValue() * 100);
                }

                // статистика браузеров
                Map<String, Double> browserStatistics = stats.getBrowserStatistics();
                writer.println("\nСтатистика браузеров (проценты):");
                for (Map.Entry<String, Double> entry : browserStatistics.entrySet()) {
                    writer.printf("  %s: %.2f%%\n", entry.getKey(), entry.getValue() * 100);
                }

                writer.println("\n" + "=" .repeat(50));
                writer.println("Анализ завершен");
            }
            System.out.println("Результаты сохранены в: " + outputPath);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // вывод в файл
    private static void printBrowserStatisticsToFile(Statistics stats, PrintWriter writer) {
        for (Map.Entry<String, Integer> entry : stats.getBrowserStats().entrySet()) {
            writer.println("  " + entry.getKey() + ": " + entry.getValue() + " запросов");
        }
    }
    private static void printOsStatisticsToFile(Statistics stats, PrintWriter writer) {
        for (Map.Entry<String, Integer> entry : stats.getOsStats().entrySet()) {
            writer.println("  " + entry.getKey() + ": " + entry.getValue() + " запросов");
        }
    }
}

// исключениe длинных строк
class LineTooLongException extends RuntimeException {
    public LineTooLongException(String message) {
        super(message);
    }
}

// Enum для методов HTTP-запросов
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
        // выражение для парсинга
        Pattern pattern;
        pattern = Pattern.compile("^(\\S+) - - \\[(.+?)] \"(\\S+) (\\S+) HTTP/\\d\\.\\d\" (\\d+) (\\d+) \"([^\"]*)\" \"([^\"]*)\"$");
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
    //public HttpMethod getMethod() { return method; }
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
    private final boolean isBot;

    public UserAgent(String userAgentString) {
        this.osType = detectOsType(userAgentString);
        this.browser = detectBrowser(userAgentString);
        this.isBot = detectBot(userAgentString);
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

    private boolean detectBot(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return false;
        }
        String ua = userAgent.toLowerCase();
        return ua.contains("bot");
    }

    // Геттеры
    public String getOsType() { return osType; }
    public String getBrowser() { return browser; }
    public boolean isBot() { return isBot; }
}

// Класс для статистики
class Statistics {
    private int totalTraffic;
    private LocalDateTime minTime;
    private LocalDateTime maxTime;
    private final Map<String, Integer> browserStats;
    private final Map<String, Integer> osStats;

    // Существующие поля страниц и статистики ОС
    private Set<String> existingPages;
    private Map<String, Integer> osFrequency;

    // Новые поля несуществующих страниц и статистики браузеров
    private Set<String> notFoundPages;
    private Map<String, Integer> browserFrequency;

    // поля для требуемых метрик
    private int humanVisits; // посещения реальными пользователями (не ботами)
    private int errorRequests; // ошибочные запросы (4xx или 5xx)
    private Set<String> uniqueHumanIPs; // уникальные IP реальных пользователей

    // Новые поля для доп методов
    private Map<Long, Integer> visitsPerSecond; // посещения по секундам (только реальные пользователи)
    private Set<String> refererDomains; // домены рефереров
    private Map<String, Integer> visitsPerUser; // посещения по пользователям (IP-адреса реальных пользователей)

    public Statistics() {
        this.totalTraffic = 0;
        this.minTime = null;
        this.maxTime = null;
        this.browserStats = new HashMap<String, Integer>();
        this.osStats = new HashMap<String, Integer>();
        this.existingPages = new HashSet<String>();
        this.osFrequency = new HashMap<String, Integer>();

        // Инициализация существующих полей
        this.notFoundPages = new HashSet<String>();
        this.browserFrequency = new HashMap<String, Integer>();

        // Инициализация новых полей
        this.humanVisits = 0;
        this.errorRequests = 0;
        this.uniqueHumanIPs = new HashSet<String>();

        // Инициализация дополнительных полей
        this.visitsPerSecond = new HashMap<Long, Integer>();
        this.refererDomains = new HashSet<String>();
        this.visitsPerUser = new HashMap<String, Integer>();
    }

    public void addEntry(LogEntry entry) {
        // Добавлен трафик
        this.totalTraffic += entry.getDataSize();

        // Обновляем min и max
        LocalDateTime entryTime = entry.getDateTime();
        if (minTime == null || entryTime.isBefore(minTime)) {
            minTime = entryTime;
        }
        if (maxTime == null || entryTime.isAfter(maxTime)) {
            maxTime = entryTime;
        }

        // статистика по браузерам и ОС
        UserAgent userAgent = entry.getUserAgent();
        String browser = userAgent.getBrowser();
        String os = userAgent.getOsType();

        browserStats.put(browser, browserStats.getOrDefault(browser, 0) + 1);
        osStats.put(os, osStats.getOrDefault(os, 0) + 1);

        // страницы с ответом 200
        if (entry.getResponseCode() == 200) {
            existingPages.add(entry.getPath());
        }

        // страницы с ответом 404
        if (entry.getResponseCode() == 404) {
            notFoundPages.add(entry.getPath());
        }

        // Собираем ОС
        osFrequency.put(os, osFrequency.getOrDefault(os, 0) + 1);

        // по браузерам
        browserFrequency.put(browser, browserFrequency.getOrDefault(browser, 0) + 1);

        // подсчеты для требуемых метрик
        boolean isHuman = !userAgent.isBot();

        // Подсчет посещений реальными пользователями
        if (isHuman) {
            humanVisits++;
            uniqueHumanIPs.add(entry.getIpAddress());

            // Подсчет посещений по секундам (только реальные пользователи)
            long secondKey = entryTime.toEpochSecond(java.time.ZoneOffset.UTC);
            visitsPerSecond.put(secondKey, visitsPerSecond.getOrDefault(secondKey, 0) + 1);

            // Подсчет посещений по пользователям (IP-адреса реальных пользователей)
            String ip = entry.getIpAddress();
            visitsPerUser.put(ip, visitsPerUser.getOrDefault(ip, 0) + 1);
        }

        // Подсчет ошибочных запросов (4xx или 5xx)
        if (entry.getResponseCode() >= 400 && entry.getResponseCode() < 600) {
            errorRequests++;
        }

        // Сбор доменов рефереров
        if (entry.getReferer() != null && !entry.getReferer().equals("-")) {
            String domain = extractDomainFromReferer(entry.getReferer());
            if (domain != null) {
                refererDomains.add(domain);
            }
        }
    }

    /**
     * Извлекает домен из referer
     * @param referer строка referer
     * @return доменное имя или null, если извлечь не удалось
     */
    private String extractDomainFromReferer(String referer) {
        try {
            // Убираем протокол и путь, оставляем только домен
            String domain = referer.replaceFirst("^(https?://)?(www\\.)?", "");
            // Убираем путь после домена
            int slashIndex = domain.indexOf('/');
            if (slashIndex != -1) {
                domain = domain.substring(0, slashIndex);
            }
            // Убираем порт если есть
            int colonIndex = domain.indexOf(':');
            if (colonIndex != -1) {
                domain = domain.substring(0, colonIndex);
            }
            return domain;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Метод расчёта пиковой посещаемости сайта (в секунду)
     * @return максимальное количество посещений за одну секунду
     */
    public int getPeakVisitsPerSecond() {
        int maxVisits = 0;
        for (int visits : visitsPerSecond.values()) {
            if (visits > maxVisits) {
                maxVisits = visits;
            }
        }
        return maxVisits;
    }

    /**
     * Метод, возвращающий список сайтов, со страниц которых есть ссылки на текущий сайт
     * @return Set<String> содержащий доменные имена рефереров
     */
    public Set<String> getRefererDomains() {
        return new HashSet<String>(refererDomains);
    }

    /**
     * Метод расчёта максимальной посещаемости одним пользователем
     * @return максимальное количество посещений одним пользователем
     */
    public int getMaxVisitsPerUser() {
        int maxVisits = 0;
        for (int visits : visitsPerUser.values()) {
            if (visits > maxVisits) {
                maxVisits = visits;
            }
        }
        return maxVisits;
    }

    /**
     * Метод подсчёта среднего количества посещений сайта за час
     * @return среднее количество посещений реальными пользователями за час
     */
    public double getAverageVisitsPerHour() {
        if (minTime == null || maxTime == null || minTime.equals(maxTime)) {
            return 0.0;
        }

        long hoursBetween = ChronoUnit.HOURS.between(minTime, maxTime);
        if (hoursBetween == 0) {
            hoursBetween = 1;
        }

        return (double) humanVisits / hoursBetween;
    }

    /**
     * Метод подсчёта среднего количества ошибочных запросов в час
     * @return среднее количество ошибочных запросов в час
     */
    public double getAverageErrorRequestsPerHour() {
        if (minTime == null || maxTime == null || minTime.equals(maxTime)) {
            return 0.0;
        }

        long hoursBetween = ChronoUnit.HOURS.between(minTime, maxTime);
        if (hoursBetween == 0) {
            hoursBetween = 1;
        }

        return (double) errorRequests / hoursBetween;
    }

    /**
     * Метод расчёта средней посещаемости одним пользователем
     * @return среднее количество посещений на одного реального пользователя
     */
    public double getAverageVisitsPerUser() {
        if (uniqueHumanIPs.isEmpty()) {
            return 0.0;
        }

        return (double) humanVisits / uniqueHumanIPs.size();
    }

    /**
     * Возврат страниц с ответом 200
     * @return Set<String> содержащий адреса существующих страниц
     */
    public Set<String> getExistingPages() {
        return new HashSet<String>(existingPages);
    }

    /**
     * Возврат страниц с ответом 404
     * @return Set<String> содержащий адреса несуществующих страниц
     */
    public Set<String> getNotFoundPages() {
        return new HashSet<String>(notFoundPages);
    }

    /**
     * Возврат ОС
     * @return Map<String, Double> где ключ - название ОС, значение - доля от общего количества
     */
    public Map<String, Double> getOsStatistics() {
        Map<String, Double> osProportions = new HashMap<String, Double>();

        // Вычисляем общее количество записей
        int totalRequests = 0;
        for (int count : osFrequency.values()) {
            totalRequests += count;
        }

        // Расчет % для каждой ос
        for (Map.Entry<String, Integer> entry : osFrequency.entrySet()) {
            String os = entry.getKey();
            int count = entry.getValue();
            double proportion = (double) count / totalRequests;
            osProportions.put(os, proportion);
        }

        return osProportions;
    }

    /**
     * Возврат браузеров
     * @return Map<String, Double> где ключ - название браузера, значение - процент от общего количества
     */
    public Map<String, Double> getBrowserStatistics() {
        Map<String, Double> browserProportions = new HashMap<String, Double>();

        // Вычисляем общее количество записей
        int totalRequests = 0;
        for (int count : browserFrequency.values()) {
            totalRequests += count;
        }

        // Расяет % для каждого браузера
        for (Map.Entry<String, Integer> entry : browserFrequency.entrySet()) {
            String browser = entry.getKey();
            int count = entry.getValue();
            double proportion = (double) count / totalRequests;
            browserProportions.put(browser, proportion);
        }

        return browserProportions;
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