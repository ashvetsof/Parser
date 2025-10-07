import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

public class Main {
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
            writer.println("Анализ лог-файла: " + inputPath);
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