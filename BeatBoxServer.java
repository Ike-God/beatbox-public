import javax.imageio.IIOException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeatBoxServer {
    private final List<ObjectOutputStream> out = new ArrayList<>();

    public static void main(String[] args) {
        new BeatBoxServer().connect();
    }

    private void connect(){
        try{
            ServerSocket serverSocket = new ServerSocket( 4242);

            ExecutorService executor = Executors.newCachedThreadPool();

            while(!serverSocket.isClosed()) {
                Socket channel = serverSocket.accept();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(channel.getOutputStream());
                out.add(objectOutputStream);

                executor.execute(new RunServer(channel));
                System.out.println("connection successful");
            }
        } catch (Exception e) {
            System.out.println("couldn't secure connection");
        }
    }
    private void tellEveryone(Object one, Object two){
        for (ObjectOutputStream obj : out){
            try {
                obj.writeObject(one);
                obj.writeObject(two);
            } catch (IOException e) {
                System.out.println("Couldn't write");
            }
        }
    }

    public  class RunServer implements Runnable{
        ObjectInputStream in;
        @Override
        public void run() {
            read();
        }
        public RunServer(Socket socket){
            try{
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.out.println("Couldn't read objects");
            }
        }
        private void read(){
            try{
                Object userMessage, sequence;

                while((userMessage = in.readObject()) != null){

                    sequence = in.readObject();

                    System.out.println("Reading two objects");

                    tellEveryone(userMessage, sequence);

                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Something went wrong");
            }
        }
    }
}
