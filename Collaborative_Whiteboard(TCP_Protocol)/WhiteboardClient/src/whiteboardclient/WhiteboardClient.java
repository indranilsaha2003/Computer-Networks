package whiteboardclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WhiteboardClient extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";  // Change to LAN server IP if needed
    private static final int PORT = 5555;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private DrawingPanel drawingPanel;
    private Color selectedColor = Color.BLACK;  // Default color
    private final AtomicBoolean isReceivingSync = new AtomicBoolean(false);
    private boolean connected = false;
    private final JLabel connectionStatus;

    public WhiteboardClient() {
        setTitle("Collaborative Whiteboard");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Buttons and Controls
        JButton drawButton = new JButton("Draw");
        JButton eraseButton = new JButton("Erase");
        JButton clearButton = new JButton("Clear");
        JButton syncButton = new JButton("Sync");
        JButton colorButton = new JButton("Color");
        JButton undoButton = new JButton("Undo");
        JButton redoButton = new JButton("Redo");
        JButton reconnectButton = new JButton("Reconnect");
        JSlider weightSlider = new JSlider(1, 10, 3);
        
        connectionStatus = new JLabel("Disconnected");
        connectionStatus.setForeground(Color.RED);

        // Action Listeners
        drawButton.addActionListener(e -> drawingPanel.setMode("DRAW"));
        eraseButton.addActionListener(e -> drawingPanel.setMode("ERASE"));

        clearButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this, 
                "Are you sure you want to clear the whiteboard for everyone?", 
                "Confirm Clear", 
                JOptionPane.YES_NO_OPTION
            );
            
            if (confirm == JOptionPane.YES_OPTION) {
                drawingPanel.clear();
                if (connected && out != null) {
                    out.println("CLEAR");
                }
            }
        });

        syncButton.addActionListener(e -> requestSync());

        colorButton.addActionListener(e -> chooseColor());

        undoButton.addActionListener(e -> {
            if (connected && out != null) {
                out.println("UNDO");
                System.out.println("Sent UNDO command to server");
            }
        });
        
        redoButton.addActionListener(e -> {
            if (connected && out != null) {
                out.println("REDO");
                System.out.println("Sent REDO command to server");
            }
        });
        
        reconnectButton.addActionListener(e -> {
            if (!connected) {
                connectToServer();
            }
        });

        weightSlider.addChangeListener(e -> drawingPanel.setStrokeWeight(weightSlider.getValue()));
        weightSlider.setPaintTicks(true);
        weightSlider.setPaintLabels(true);
        weightSlider.setMajorTickSpacing(3);
        weightSlider.setMinorTickSpacing(1);

        // Add Controls to the Panel
        controlPanel.add(drawButton);
        controlPanel.add(eraseButton);
        controlPanel.add(new JLabel("Weight:"));
        controlPanel.add(weightSlider);
        controlPanel.add(clearButton);
        controlPanel.add(syncButton);
        controlPanel.add(colorButton);
        controlPanel.add(undoButton);
        controlPanel.add(redoButton);
        controlPanel.add(reconnectButton);
        controlPanel.add(connectionStatus);

        add(controlPanel, BorderLayout.NORTH);

        connectToServer();
        
        // Add window closing event to clean up resources
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupConnection();
            }
        });
    }

    // Connect to the server
    private void connectToServer() {
        try {
            // Close any existing connection first
            cleanupConnection();
            
            // Connect to server
            socket = new Socket(SERVER_IP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            connected = true;
            connectionStatus.setText("Connected");
            connectionStatus.setForeground(Color.GREEN);
            
            System.out.println("Connected to server at " + SERVER_IP + ":" + PORT);
            
            // Start listening for server data
            new Thread(this::listenForServerData).start();
            
            // Sync immediately after connecting
            requestSync();
            
        } catch (IOException e) {
            connected = false;
            connectionStatus.setText("Connection Failed");
            connectionStatus.setForeground(Color.RED);
            
            System.err.println("Connection error: " + e.getMessage());
            JOptionPane.showMessageDialog(
                this, 
                "Unable to connect to server: " + e.getMessage(), 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private void cleanupConnection() {
        connected = false;
        
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
        
        connectionStatus.setText("Disconnected");
        connectionStatus.setForeground(Color.RED);
    }

    // Listen for server data
    private void listenForServerData() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                final String receivedMessage = message;
                SwingUtilities.invokeLater(() -> processServerMessage(receivedMessage));
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("Connection lost: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    connected = false;
                    connectionStatus.setText("Connection Lost");
                    connectionStatus.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(
                        this, 
                        "Connection to server lost. Use Reconnect button to try again.", 
                        "Connection Lost", 
                        JOptionPane.WARNING_MESSAGE
                    );
                });
            }
        }
    }
    
    private void processServerMessage(String message) {
        if (message.equals("SYNC_START")) {
            System.out.println("Starting to receive sync data");
            isReceivingSync.set(true);
            drawingPanel.clear();
        } else if (message.equals("SYNC_END")) {
            System.out.println("Finished receiving sync data");
            isReceivingSync.set(false);
        } else if (message.equals("UNDO")) {
            System.out.println("Received UNDO command from server");
            drawingPanel.undoLastStroke();
        } else if (message.equals("REDO")) {
            System.out.println("Received REDO command from server");
            drawingPanel.redoLastStroke();
        } else if (message.equals("CLEAR")) {
            System.out.println("Received CLEAR command from server");
            drawingPanel.clear();
        } else {
            drawingPanel.processStrokeFromServer(message);
        }
    }

    // Request sync for previous canvas data
    private void requestSync() {
        if (connected && out != null) {
            out.println("SYNC");
            System.out.println("Sent SYNC request to server");
        } else {
            JOptionPane.showMessageDialog(
                this, 
                "Not connected to server", 
                "Connection Error", 
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    // Choose color using JColorChooser
    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Drawing Color", selectedColor);
        if (newColor != null) {
            selectedColor = newColor;
        }
    }

    // DrawingPanel class to handle canvas operations
    private class DrawingPanel extends JPanel {
        private int prevX, prevY;
        private String mode = "DRAW";
        private int strokeWeight = 3;
        private Image canvas;
        private Graphics2D g2;

        // List of strokes to handle undo properly
        private final List<String> strokes = new ArrayList<>();
        
        // Stack to store undone strokes for the redo functionality
        private final List<String> undoneStrokes = new ArrayList<>();
        
        // Store the last stroke segments for continuous drawing
        private final List<String> currentStrokeSegments = new ArrayList<>();

        public DrawingPanel() {
            setBackground(Color.WHITE);

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (!connected) return;
                    
                    int x = e.getX(), y = e.getY();

                    if (out != null) {
                        String colorStr = selectedColor.getRed() + "," +
                                selectedColor.getGreen() + "," +
                                selectedColor.getBlue();

                        String message = mode + "," + strokeWeight + "," + colorStr + "," +
                                prevX + "," + prevY + "," + x + "," + y;

                        out.println(message);
                        
                        // Add to current stroke segments
                        currentStrokeSegments.add(message);
                        
                        // Draw locally
                        drawStrokeFromString(message);
                    }

                    prevX = x;
                    prevY = y;
                }
            });

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (!connected) return;
                    
                    prevX = e.getX();
                    prevY = e.getY();
                    currentStrokeSegments.clear();
                }
                
                public void mouseReleased(MouseEvent e) {
                    if (!connected) return;
                    
                    // When mouse is released, combine all segments into one stroke
                    if (!currentStrokeSegments.isEmpty()) {
                        String completeStroke = String.join("|", currentStrokeSegments);
                        
                        // Send the complete stroke to the server
                        if (out != null) {
                            out.println(completeStroke);
                        }
                        
                        // Add the complete stroke to history
                        strokes.add(completeStroke);
                        System.out.println("Added complete stroke. Total strokes: " + strokes.size());
                        
                        // Clear undone strokes when a new stroke is added
                        if (!undoneStrokes.isEmpty()) {
                            undoneStrokes.clear();
                            System.out.println("Cleared redo history after new stroke");
                        }
                        
                        // Clear current segments
                        currentStrokeSegments.clear();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvas == null) {
                canvas = createImage(getWidth(), getHeight());
                g2 = (Graphics2D) canvas.getGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g.drawImage(canvas, 0, 0, null);
        }

        // Process stroke from server
        public void processStrokeFromServer(String message) {
            try {
                if (message.contains("|")) {
                    // This is a multi-segment stroke
                    if (!isReceivingSync.get()) {
                        strokes.add(message);
                    }
                    
                    String[] segments = message.split("\\|");
                    for (String segment : segments) {
                        drawStrokeFromString(segment);
                    }
                } else {
                    // Only add individual segments to the history if we're not in sync mode
                    // and it's not part of a multi-segment stroke
                    if (!isReceivingSync.get()) {
                        drawStrokeFromString(message);
                    }
                }
                repaint();
            } catch (Exception e) {
                System.err.println("Error processing stroke from server: " + e.getMessage());
            }
        }

        // Clear the canvas and stroke history
        public void clear() {
            if (canvas != null) {
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                strokes.clear();
                undoneStrokes.clear();
                currentStrokeSegments.clear();
                repaint();
                System.out.println("Canvas cleared. All strokes and redo history cleared.");
            }
        }

        // Undo the last stroke
        public void undoLastStroke() {
            if (!strokes.isEmpty()) {
                // Get the last stroke and move it to the undone strokes list
                String lastStroke = strokes.remove(strokes.size() - 1);
                undoneStrokes.add(lastStroke);
                System.out.println("Stroke undone. Remaining strokes: " + strokes.size() + 
                                   ", Undone strokes: " + undoneStrokes.size());
                redrawAllStrokes();
            } else {
                System.out.println("Nothing to undo - stroke list is empty");
            }
        }
        
        // Redo the last undone stroke
        public void redoLastStroke() {
            if (!undoneStrokes.isEmpty()) {
                // Move the last undone stroke back to the strokes list
                String lastUndoneStroke = undoneStrokes.remove(undoneStrokes.size() - 1);
                strokes.add(lastUndoneStroke);
                System.out.println("Stroke redone. Current strokes: " + strokes.size() + 
                                   ", Remaining undone strokes: " + undoneStrokes.size());
                redrawAllStrokes();
            } else {
                System.out.println("Nothing to redo - undone stroke list is empty");
            }
        }

        // Redraw the canvas with all strokes
        private void redrawAllStrokes() {
            if (g2 == null) return;
            
            // Clear the canvas
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            // Redraw all remaining strokes
            for (String stroke : strokes) {
                if (stroke.contains("|")) {
                    // Multi-segment stroke
                    String[] segments = stroke.split("\\|");
                    for (String segment : segments) {
                        drawStrokeFromString(segment);
                    }
                } else {
                    // Single segment stroke
                    drawStrokeFromString(stroke);
                }
            }
            repaint();
        }

        // Draw from a stored string format
        private void drawStrokeFromString(String message) {
            String[] parts = message.split(",");
            
            if (parts.length < 7) {
                System.out.println("Invalid stroke format: " + message);
                return;
            }

            try {
                String mode = parts[0];
                int weight = Integer.parseInt(parts[1]);
                
                // Extract color
                Color color;
                int colorIndex = 2;
                int r = Integer.parseInt(parts[colorIndex++]);
                int g = Integer.parseInt(parts[colorIndex++]);
                int b = Integer.parseInt(parts[colorIndex++]);
                color = new Color(r, g, b);
                
                // Extract coordinates
                int x1 = Integer.parseInt(parts[colorIndex++]);
                int y1 = Integer.parseInt(parts[colorIndex++]);
                int x2 = Integer.parseInt(parts[colorIndex++]);
                int y2 = Integer.parseInt(parts[colorIndex]);
                
                // Draw the line
                drawLine(x1, y1, x2, y2, mode, weight, color);
            } catch (Exception e) {
                System.err.println("Error parsing stroke: " + message);
                e.printStackTrace();
            }
        }

        // Draw a line on the canvas
        private void drawLine(int x1, int y1, int x2, int y2, String mode, int weight, Color color) {
            if (g2 == null) return;

            g2.setStroke(new BasicStroke(weight, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if ("DRAW".equals(mode)) {
                g2.setColor(color);
            } else if ("ERASE".equals(mode)) {
                g2.setColor(getBackground());
            }

            g2.drawLine(x1, y1, x2, y2);
            repaint();
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public void setStrokeWeight(int weight) {
            this.strokeWeight = weight;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new WhiteboardClient().setVisible(true));
    }
}