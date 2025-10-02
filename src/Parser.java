import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    public static void main(String[] args) {
        // относительный путь
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
        try (
                FileReader fileReader = new FileReader(file);
                BufferedReader reader = new BufferedReader(fileReader)
        ) {
            int totalLines = 0;
            int googlebotCount = 0;
            int yandexbotCount = 0;
            String line;
            System.out.println("Подсчет...");
            while ((line = reader.readLine()) != null) {
                int length = line.length();
                totalLines++;
                // проверка длины строки
                if (length > 1024) {
                    throw new LineTooLongException(
                            "Строка #" + totalLines + " превышает 1024 символа. Длина: " + length
                    );
                }
                // Разделение строк на составляющие+анализ агента
                String userAgent = extractUserAgent(line);
                if (userAgent != null) {
                    String program = extractProgramFromUserAgent(userAgent);
                    if ("Googlebot".equals(program)) {
                        googlebotCount++;
                    } else if ("YandexBot".equals(program)) {
                        yandexbotCount++;
                    }
                }
                // вывод каждые 1000 строк
                if (totalLines % 1000 == 0) {
                    System.out.println("Обработано строк: " + totalLines);
                }
            }
            if (totalLines == 0) {
                System.out.println("Файл пуст.");
            } else {
                System.out.println("\nРезультаты анализа:");
                System.out.println("Всего строк: " + totalLines);

                // подсчет и вывод долей
                double googlebotPercentage = (double) googlebotCount / totalLines * 100;
                double yandexbotPercentage = (double) yandexbotCount / totalLines * 100;

                System.out.printf("Запросы от гугл бота: %d (%.2f%%)\n", googlebotCount, googlebotPercentage);
                System.out.printf("Запросы от яндекс бота: %d (%.2f%%)\n", yandexbotCount, yandexbotPercentage);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    /**
     * Извлечение User-Agent
     */
    private static String extractUserAgent(String logLine) {
        // извлечение User-Agent
        Pattern pattern = Pattern.compile("\" \"([^\"]*)\"$");
        Matcher matcher = pattern.matcher(logLine);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * выдает название программы из User-Agent
     */
    private static String extractProgramFromUserAgent(String userAgent) {
        try {
            // поиск части в первых скобках
            int startBracket = userAgent.indexOf('(');
            int endBracket = userAgent.indexOf(')');
            if (startBracket == -1 || endBracket == -1 || startBracket >= endBracket) {
                return null;
            }
            String firstBrackets = userAgent.substring(startBracket + 1, endBracket);
            // делим вывол точкой с запятой
            String[] parts = firstBrackets.split(";");
            if (parts.length >= 2) {
                // удаление пробелов
                String fragment = parts[1].trim();
                // Отделяем до слэша
                int slashIndex = fragment.indexOf('/');
                if (slashIndex != -1) {
                    return fragment.substring(0, slashIndex).trim();
                }
            }
        } catch (Exception e) {
            // В случае ошибки парсинга
        }
        return null;
    }
}

