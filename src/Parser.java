import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
public class Parser {
    public static void main(String[] args) {
        // относительный путь
        String path;
        if (args.length > 0) {
            path = args[0];
        } else {
            // eсли нет, то файл в текущей директории
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
            int maxLength = 0;
            int minLength = Integer.MAX_VALUE;
            String line;
            System.out.println("посчет файла...");
            while ((line = reader.readLine()) != null) {
                int length = line.length();
                totalLines++;

                maxLength = Math.max(maxLength, length);
                minLength = Math.min(minLength, length);

                if (length > 1024) {
                    throw new LineTooLongException(
                            "Строка #" + totalLines + " превышает 1024 символа. Длина: " + length
                    );
                }
                // Вывод каждые 1000 строк
                if (totalLines % 1000 == 0) {
                    System.out.println("Обработано строк: " + totalLines);
                }
            }
            if (totalLines == 0) {
                minLength = 0;
                System.out.println("Файл пуст.");
            } else {
                System.out.println("\nРезультаты анализа:");
                System.out.println("Всего строк: " + totalLines);
                System.out.println("Длина самой длинной строки: " + maxLength);
                System.out.println("Длина самой короткой строки: " + minLength);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}