import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main implements Runnable {
    private static ExecutorService pool = Executors.newFixedThreadPool(20);
    private static ExecutorService cachedThread = Executors.newCachedThreadPool();
    static int port;
    final static int bufferSize = 32768;
    static byte[] c = new byte[bufferSize];

    public static void main(String[] args) {
        while (port < 1025 || port > 65000) {
            System.out.println("Введите номер порта(целое число, большее 1024, но меньшее 65000)");
            Scanner scanner = new Scanner(System.in);
            if (scanner.hasNext()) {
                if (scanner.hasNextInt()) {
                    port = scanner.nextInt();
                }
            } else {
                System.out.println("Я не люблю сочетание ctrl+D(");
                System.exit(0);
            }
        }
        Main.start();
    }

    public static void start() {
        Thread stopServer = new Thread(Main::stopServer);
        stopServer.setDaemon(false);
        Thread serverWork = new Thread(() -> {
            try {
                serverWork();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverWork.setDaemon(true);
        stopServer.start();
        serverWork.start();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return o.readObject();
            }
        }
    }


    public static void stopServer() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите help для получения списка команд на сервере");
        while (true) {
            if (scanner.hasNext()) {
                String commandWords[] = scanner.nextLine().split(" ");
                switch (commandWords[0]) {
                    case ("help"): {
                        System.out.println("help: получить список команд на сервере\n"+
                                "show_objects: показать все хранящиеся объекты\n"+
                                "show_owners: показать всех пользователей\n"+
                                "exit: прекратить работу сервера\n" +
                                "remove_by_id id: удалить объект по его id\n" +
                                "remove_owner owner'sName-удалить пользователя из БД");
                        break;
                    }
                    case ("show_objects"):
                        for (MusicBand band:CommandExecution.set) {
                            System.out.println(band);
                        }
                        break;
                    case("show_owners"):
                        for (String owner:CommandExecution.owners.keySet()) {
                            System.out.println(owner);
                        }
                        break;
                    case ("exit"): {
                        System.exit(0);
                        break;
                    }
                    case ("remove_by_id"):
                        if (commandWords[commandWords.length - 1].equals("remove_by_id")) {
                            System.out.println("А что удалять?");
                        } else {
                            try {
                                System.out.println(CommandExecution.adminsRemoveObject(Integer.parseInt(commandWords[commandWords.length - 1])));
                            } catch (Exception e) {
                                System.out.println("Попробуйте еще раз ввести команду, используя число в качесте id");
                            }
                        }
                        break;
                    case("remove_owner"):
                        if (commandWords[commandWords.length - 1].equals("remove_by_id")) {
                            System.out.println("А кого удалять?");
                        } else{
                            System.out.println(CommandExecution.adminsRemoveOwner(commandWords[commandWords.length - 1]));
                        }
                        break;
                    default:
                        System.out.println("Такой команды  у меня нет");
                        break;
                }
            }else {
                System.out.println("Не люблю я сочетание ctrl+D(");
                System.exit(0);
            }
        }
    }

    public static void serverWork() {
        pool.execute(() -> new Thread(new Main()).start());
        cachedThread.submit(() -> {
            CommandExecution.start();
            SocketAddress socketAddress = new InetSocketAddress(port);//комбинация IP-адрес+порт
            DatagramChannel datagramChannel = DatagramChannel.open(); //Запускаем сервер
            try {
                datagramChannel.bind(socketAddress); //Задаем его адрес
            } catch (BindException e) {
                System.out.println("Ой, а порт занят, попробуйте заново");
            }
            while (true) {
                byte[] b = new byte[bufferSize];
                ByteBuffer byteBuffer = ByteBuffer.wrap(b);//Преварщаем массив в буфер
                byteBuffer.clear();
                socketAddress = datagramChannel.receive(byteBuffer);//получаем запрос от клиента
                Commands command = (Commands) deserialize(b);
                command = Identifier.Identify(command);
                System.out.println(command);
                send(command, datagramChannel, socketAddress);
            }
        });
    }

    public static void send(Commands command, DatagramChannel datagramChannel, SocketAddress socketAddress) throws Exception {
        byte[] b = serialize(command);
        Arrays.fill(c, (byte) 0);
        System.arraycopy(b, 0, c, 0, b.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(c);
        byteBuffer.flip();
        byteBuffer = ByteBuffer.wrap(c);
        int i = datagramChannel.send(byteBuffer, socketAddress);//отправляем обработанный резуьтат
        System.out.println(i + " байтов информации отправлено");
    }

    public static byte[] serialize(Commands command) throws IOException {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(command);
            }
            return b.toByteArray();
        }
    }

    @Override
    public void run() {

    }
}

