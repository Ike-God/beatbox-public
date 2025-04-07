import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeatBox{
    private ArrayList<JCheckBox> checkboxList;
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private JFrame frame;
    private JTextArea chat;
    private Socket socket;
    private JList<String> incoming;
    private Vector<String> listVector = new Vector<>();
    private HashMap<String, boolean[]> chatMap = new HashMap<>();
    private String username;
    private int nextNum;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-hat", "Open Hi-hat", "Acoustic Snare", "Crush Cymbal",
    "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
    "High Agogo", "Open High Conga"};

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        new BeatBox().startUp();
    }

    private void setUpMidi(){
        try{
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void makeTracks(int[] list){
        for (int i = 0; i < 16; i++){

            int key = list[i];

            if (key != 0){
                track.add(makeEvent(ShortMessage.NOTE_ON, 9, key, 100, i));
                track.add(makeEvent(ShortMessage.NOTE_OFF, 9, key, 100, i +1));
            }
        }
    }

    private void changeTempo(float tempoMultiplier){
        float tempoFactor = sequencer.getTempoFactor();
        sequencer.setTempoFactor(tempoFactor * tempoMultiplier);
    }

    private void buildTrackAndStart(){
        //get an int array to store instrument
        int[] tracklist;

        int key;

        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++){
            tracklist = new int[16];
            key = instruments[i];

            for (int j = 0; j < 16; j++){

                JCheckBox jCheckBox = checkboxList.get(j + 16 * i);

                if (jCheckBox.isSelected()){
                    tracklist[j] = key;

                }else{
                    tracklist[j] = 0;
                }
            }
            System.out.println(Arrays.toString(tracklist));
            makeTracks(tracklist);
            track.add(makeEvent(ShortMessage.CONTROL_CHANGE, 1, 127, 0, 16));
        }

        track.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 9, 1, 0, 15));

        try{
            //add sequence to sequencer
            sequencer.setSequence(sequence);
            //loop continuously
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            //set the tempo
            sequencer.setTempoInBPM(120);
            //start the sequence
            sequencer.start();

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void writeFile() {
        boolean[] checkBoxState = new boolean[256];

        for (int i = 0; i < 256; i++){
            JCheckBox checkBox = checkboxList.get(i);

            if (checkBox.isSelected()) {
                checkBoxState[i] = true;
                System.out.println("checkbox state: "+ Arrays.toString(checkBoxState));
            }
        }
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("Checkbox.ser"))){
            os.writeObject(checkBoxState);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public void readFile(){
        boolean[] checkBoxState = null;

        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream("Checkbox.ser"))){
            checkBoxState = (boolean[]) is.readObject();
            System.out.println(Arrays.toString(checkBoxState));
        }catch (Exception e){
            e.printStackTrace();
        }

        for (int i = 0; i < 256; i++){
            JCheckBox check = checkboxList.get(i);
            check.setSelected(checkBoxState[i]);
        }
        sequencer.stop();
        buildTrackAndStart();
    }
    private void makeFile(File file){
        boolean[] checkBoxState = new boolean[256];

        for (int i = 0; i < 256; i++){
            JCheckBox checkBox = checkboxList.get(i);
            if (checkBox.isSelected()) {
                checkBoxState[i] = true;
                System.out.println("checkbox state: "+ Arrays.toString(checkBoxState));
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (boolean state : checkBoxState){
                writer.write(state + "\n");
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void getFile(File file){
        ArrayList<String> checkBoxState = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line;
            while((line = reader.readLine()) != null){
                checkBoxState.add(line);
            }
            System.out.println(checkBoxState);
        }catch (Exception e){
            e.printStackTrace();
        }

        for (int i = 0; i < 256; i++){
            JCheckBox check = checkboxList.get(i);

            check.setSelected(Boolean.parseBoolean(checkBoxState.get(i)));
        }
        sequencer.stop();
        buildTrackAndStart();
    }
    private void saveWindow(){
        JFileChooser  chooser = new JFileChooser();
        chooser.showSaveDialog(frame);
        makeFile(chooser.getSelectedFile());
    }
    private void openWindow(){
        JFileChooser chooser = new JFileChooser();
        chooser.showOpenDialog(frame);
        getFile(chooser.getSelectedFile());
    }

    private void buildGUI(){
        frame = new JFrame("Cyber BeatBox");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);

        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Box buttonBox = getBox();

        background.add(BorderLayout.EAST, buttonBox);

        //get beat names
        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (String beatName : instrumentNames){
            JLabel names = new JLabel(beatName);
            names.setBorder(BorderFactory.createEmptyBorder(4,1,4,1));
            nameBox.add(names);
        }
        background.add(BorderLayout.WEST, nameBox);

        frame.getContentPane().add(background);

        //create a gridlayout to hold checkboxes
        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);

        JPanel mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        checkboxList = new ArrayList<>();

        for (int i = 0; i < 256; i++){
            JCheckBox jc = new JCheckBox();
            checkboxList.add(jc);
            jc.setSelected(false);
            mainPanel.add(jc);
        }

        setUpMidi();

        frame.setBounds(50, 50,300, 300);
        frame.pack();
        frame.setVisible(true);
    }

    private Box getBox() {
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        //get the buttons
        JButton start = new JButton("Start");
        start.addActionListener(event -> buildTrackAndStart());
        buttonBox.add(start);

        JButton stop = new JButton("Stop");
        stop.addActionListener(event -> sequencer.stop());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Up Tempo");
        upTempo.addActionListener(event -> changeTempo(1.03f));
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Down Tempo");
        downTempo.addActionListener(event -> changeTempo(0.97f));
        buttonBox.add(downTempo);

        JButton save = new JButton("Save Pattern");
        save.addActionListener(event -> saveWindow());
        buttonBox.add(save);

        JButton restore = new JButton("Restore");
        restore.addActionListener(event -> openWindow());
        buttonBox.add(restore);

        JButton randomSong = new JButton("Play Random");
        randomSong.addActionListener(e -> randomBeats());
        buttonBox.add(randomSong);

        JButton clearScreen = new JButton("Clear Screen");
        clearScreen.addActionListener(e -> clearScreen());
        buttonBox.add(clearScreen);

        //build chat area
        JButton sendIt = new JButton("Send");
        sendIt.addActionListener(e -> sendMessageAndTrack());
        buttonBox.add(sendIt);

        chat = new JTextArea(1, 3);
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        JScrollPane chatPane = new JScrollPane(chat);
        buttonBox.add(chatPane);

        //create view chat area
        incoming = new JList<>();
        incoming.addListSelectionListener(new MyListSelection());
        incoming.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane viewPane = new JScrollPane(incoming);
        buttonBox.add(viewPane);
        incoming.setListData(listVector);

        return buttonBox;
    }
    private void startUp(){
        try{
            socket = new Socket("127.0.0.1", 4242);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(new RemoteReader());
            executor.shutdown();
            System.out.println("connection successful");
        } catch (IOException e) {
            System.out.println("Something went wrong with connection");
        }

        buildGUI();
    }

    private void sendMessageAndTrack(){
        String message = chat.getText();
        boolean[] selected = new boolean[256];
        for(int i = 0; i < 256; i++){
            JCheckBox newBox = checkboxList.get(i);
            if (newBox.isSelected()){
                selected[i] = true;
            }
        }

        try{
            if (!message.isEmpty()) {
                out.writeObject(message);
                out.writeObject(selected);
            }
        } catch (IOException e) {
            System.out.println("Something went wrong with connection");
        }
        chat.setText("");
    }

    public class MyListSelection implements ListSelectionListener{
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()){
                String selected = incoming.getSelectedValue();

                ArrayList<Boolean> state = new ArrayList<>();

                for (int i = 0; i < 256; i++){
                    JCheckBox check = checkboxList.get(i);

                    if (check.isSelected()){
                        state.add(true);
                        System.out.println("see " + state);
                    }
                }
                if (!state.isEmpty()){
                    showPopUp(selected);
                }else {
                    setCheckBoxState(selected);
                }

            }
        }
    }

    private void showPopUp(String selected){
        int  optionPane = JOptionPane.showConfirmDialog(frame, "Playing this beat pattern would delete current pattern.\n" +
        "Would you like to save the current beat pattern before you proceed?");

        if (optionPane == 0){
            saveWindow();
            setCheckBoxState(selected);
        }else if(optionPane == 1) {
            setCheckBoxState(selected);
        }
    }

    private void setCheckBoxState(String selected){
        if (selected != null){
            boolean[] chat = chatMap.get(selected);
            remakeState(chat);

            sequencer.stop();
            buildTrackAndStart();
        }
    }

    private void randomBeats(){
        Random random = new Random();

        for (int i = 0; i < 256; i++){
            JCheckBox check = checkboxList.get(i);
            check.setSelected(random.nextBoolean());

            System.out.println(check.isSelected());
        }
        sequencer.stop();
        buildTrackAndStart();
    }
    private void clearScreen(){
        for (int i = 0; i < 256; i++){
            JCheckBox check = checkboxList.get(i);
            check.setSelected(false);
        }
    }

    private void remakeState(boolean[] checkboxState){
        for (int i = 0; i < 256; i++){
            JCheckBox check = checkboxList.get(i);
            check.setSelected(checkboxState[i]);
        }
    }
    public class RemoteReader implements Runnable {

        @Override
        public void run() {
            readFromServer();
        }

        private void readFromServer(){
            try{
                Object obj;
                while((obj = in.readObject()) != null){

                    String userMessage = (String) obj;
                    boolean[] checkboxState = (boolean[]) in.readObject();

                    System.out.println(userMessage);
                    System.out.println(Arrays.toString(checkboxState));

                    chatMap.put(userMessage, checkboxState);

                    listVector.add(userMessage);
                    incoming.setListData(listVector);

                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Something went wrong");
            }
        }
    }

    public static MidiEvent makeEvent(int command, int channel, int one, int two, int tick){
        MidiEvent event = null;

        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(command, channel, one, two);
            event = new MidiEvent(msg, tick);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }
}
