import java.io.File;
import java.util.Scanner;
public class Parser {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int correctFileCount = 0; // счетчик указанных файлов

        // цикл для запроса пути к файлу
        while (true) {
            System.out.println("Указать путь к файлу (или 'exit' для выхода):");
            String path = scanner.nextLine();

            // Проверка выхода
            if ("exit".equalsIgnoreCase(path)) {
                System.out.println("Программа завершена.");
                break;
            }
            // Создание объекта файла и проверка
            File file = new File(path);
            boolean fileExists = file.exists();
            boolean isDirectory = file.isDirectory();

            // Проверка if-else
            if (!fileExists || isDirectory) {
                if (!fileExists) {
                    System.out.println("Файл не существует: " + path);
                } else {
                    System.out.println("Указанный путь ведет к папке: " + path);
                }
                continue; // переходим к следующей итерации цикла
            } else {
                // Файл существует и это именно файл
                correctFileCount++;
                System.out.println("Путь прописан верно");
                System.out.println("Это файл номер " + correctFileCount);

                // Здесь можно добавить код для парсинга файла
                // parseLogFile(file);
            }
        }
        scanner.close();
    }
    // Метод парсинга
    private static void parseLogFile(File file) {
        System.out.println("Парсинг файла: " + file.getName());
        // Реализация парсинга лог-файла
        // Path C:\Users\ASHvetsov\git\Parser\logs.txt
    }
}