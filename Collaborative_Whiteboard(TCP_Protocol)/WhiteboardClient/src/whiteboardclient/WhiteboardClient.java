package whiteboardclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardClient extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";  // Change to LAN server IP if needed
    private static final int PORT = 5555;
    private Socket socket;
    private PrintWriter out;
    private DrawingPanel drawingPanel;
    private Color selectedColor = Color.BLACK;  // Default color
    private boolean performingLocalUndo = false; // Flag to prevent duplicate undos

    public WhiteboardClient() {
        setTitle("Collaborative Whiteboard with Undo");
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
        JSlider weightSlider = new JSlider(1, 10, 3);

        // Action Listeners
        drawButton.addActionListener(e -> drawingPanel.setMode("DRAW"));
        eraseButton.addActionListener(e -> drawingPanel.setMode("ERASE"));

        clearButton.addActionListener(e -> {
            drawingPanel.clear();
            if (out != null) {
                out.println("CLEAR");
            }
        });

        syncButton.addActionListener(e -> requestSync());

        colorButton.addActionListener(e -> chooseColor());

        // Fixed undo button handling
        undoButton.addActionListener(e -> {
            if (out != null) {
                // Just send the UNDO command to the server
                // The actual undo will happen when we receive it back
                out.println("UNDO");
                System.out.println("Sent UNDO command to server");
            }
        });

        weightSlider.addChangeListener(e -> drawingPanel.setStrokeWeight(weightSlider.getValue()));

        // Add Controls to the Panel
        controlPanel.add(drawButton);
        controlPanel.add(eraseButton);
        controlPanel.add(new JLabel("Weight:"));
        controlPanel.add(weightSlider);
        controlPanel.add(clearButton);
        controlPanel.add(syncButton);
        controlPanel.add(colorButton);
        controlPanel.add(undoButton);

        add(controlPanel, BorderLayout.NORTH);

        connectToServer();
        new Thread(this::listenForServerData).start();
    }

    // Connect to the server
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server at " + SERVER_IP + ":" + PORT);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to server", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    // Listen for server data
    private void listenForServerData() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                final String receivedMessage = message;
                SwingUtilities.invokeLater(() -> {
                    if (receivedMessage.equals("UNDO")) {
                        System.out.println("Received UNDO command from server");
                        drawingPanel.undoLastStroke();
                    } else if (receivedMessage.equals("CLEAR")) {
                        System.out.println("Received CLEAR command from server");
                        drawingPanel.clear();
                    } else {
                        drawingPanel.processStrokeFromServer(receivedMessage);
                    }
                });
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        }
    }

    // Request sync for previous canvas data
    private void requestSync() {
        if (out != null) {
            out.println("SYNC");
            System.out.println("Sent SYNC request to server");
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
        
        // Store the last stroke segments for continuous drawing
        private final List<String> currentStrokeSegments = new ArrayList<>();

        public DrawingPanel() {
            setBackground(Color.WHITE);

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
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
                    prevX = e.getX();
                    prevY = e.getY();
                    currentStrokeSegments.clear();
                }
                
                public void mouseReleased(MouseEvent e) {
                    // When mouse is released, combine all segments into one stroke
                    if (!currentStrokeSegments.isEmpty()) {
                        // Add the complete stroke to history
                        strokes.add(String.join("|", currentStrokeSegments));
                        System.out.println("Added complete stroke. Total strokes: " + strokes.size());
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
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g.drawImage(canvas, 0, 0, null);
        }

        // Process stroke from server
        public void processStrokeFromServer(String message) {
            if (message.contains("|")) {
                // This is a multi-segment stroke
                strokes.add(message);
                String[] segments = message.split("\\|");
                for (String segment : segments) {
                    drawStrokeFromString(segment);
                }
            } else {
                // Single segment stroke
                strokes.add(message);
                drawStrokeFromString(message);
            }
            repaint();
        }

        // Clear the canvas and stroke history
        public void clear() {
            if (canvas != null) {
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                strokes.clear();
                currentStrokeSegments.clear();
                repaint();
                System.out.println("Canvas cleared. Strokes: " + strokes.size());
            }
        }

        // Undo the last stroke
        public void undoLastStroke() {
            if (!strokes.isEmpty()) {
                strokes.remove(strokes.size() - 1);
                System.out.println("Stroke removed. Remaining strokes: " + strokes.size());
                redrawAllStrokes();
            } else {
                System.out.println("Nothing to undo - stroke list is empty");
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

            g2.setStroke(new BasicStroke(weight));

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
        SwingUtilities.invokeLater(() -> new WhiteboardClient().setVisible(true));
    }
}
