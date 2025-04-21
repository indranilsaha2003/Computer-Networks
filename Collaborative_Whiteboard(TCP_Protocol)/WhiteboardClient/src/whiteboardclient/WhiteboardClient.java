package whiteboardclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardClient extends JFrame {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5555;
    private Socket socket;
    private PrintWriter out;
    private DrawingPanel drawingPanel;
    private Color selectedColor = Color.BLACK;

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

        // FOCUS HERE: Simplified undo button handler
        undoButton.addActionListener(e -> {
            System.out.println("Undo button clicked. Strokes before: " + drawingPanel.strokes.size());
            drawingPanel.undoLastStroke();
            System.out.println("Strokes after: " + drawingPanel.strokes.size());
            if (out != null) {
                out.println("UNDO");
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

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to server", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void listenForServerData() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("UNDO")) {
                    SwingUtilities.invokeLater(() -> drawingPanel.undoLastStroke());
                } else if (message.equals("CLEAR")) {
                    SwingUtilities.invokeLater(() -> drawingPanel.clear());
                } else {
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> drawingPanel.processStrokeFromServer(finalMessage));
                }
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        }
    }

    private void requestSync() {
        if (out != null) {
            out.println("SYNC");
        }
    }

    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Drawing Color", selectedColor);
        if (newColor != null) {
            selectedColor = newColor;
        }
    }

    // FOCUS HERE: Simplified DrawingPanel focusing on local undo
    private class DrawingPanel extends JPanel {
        private int prevX, prevY;
        private String mode = "DRAW";
        private int strokeWeight = 3;
        private Image canvas;
        private Graphics2D g2;

        // Store stroke information for undo
        private final List<String> strokes = new ArrayList<>();
        
        // Store the last stroke as segments for continuous drawing
        private final List<StrokeSegment> currentStrokeSegments = new ArrayList<>();
        
        private class StrokeSegment {
            int x1, y1, x2, y2;
            String mode;
            int weight;
            Color color;
            
            StrokeSegment(int x1, int y1, int x2, int y2, String mode, int weight, Color color) {
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
                this.mode = mode;
                this.weight = weight;
                this.color = color;
            }
            
            String serialize() {
                return mode + "," + weight + "," + 
                       color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," +
                       x1 + "," + y1 + "," + x2 + "," + y2;
            }
        }

        public DrawingPanel() {
            setBackground(Color.WHITE);

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    int x = e.getX(), y = e.getY();
                    
                    // Create a new stroke segment
                    StrokeSegment segment = new StrokeSegment(
                        prevX, prevY, x, y, mode, strokeWeight, selectedColor
                    );
                    
                    // Draw the segment
                    drawLine(segment.x1, segment.y1, segment.x2, segment.y2, 
                             segment.mode, segment.weight, segment.color);
                    
                    // Add to current stroke segments
                    currentStrokeSegments.add(segment);
                    
                    // Send to server
                    if (out != null) {
                        out.println(segment.serialize());
                    }
                    
                    prevX = x;
                    prevY = y;
                }
            });

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    prevX = e.getX();
                    prevY = e.getY();
                    // Clear the current stroke segments for a new stroke
                    currentStrokeSegments.clear();
                }
                
                public void mouseReleased(MouseEvent e) {
                    // When mouse is released, add all segments as one stroke
                    if (!currentStrokeSegments.isEmpty()) {
                        StringBuilder strokeData = new StringBuilder("MULTISEGMENT");
                        for (StrokeSegment segment : currentStrokeSegments) {
                            strokeData.append("|").append(segment.serialize());
                        }
                        strokes.add(strokeData.toString());
                        System.out.println("Added stroke. Total strokes: " + strokes.size());
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

        // Process stroke data from the server
        public void processStrokeFromServer(String message) {
            if (message.startsWith("MULTISEGMENT")) {
                // Handle multi-segment stroke
                strokes.add(message);
                String[] segments = message.split("\\|");
                for (int i = 1; i < segments.length; i++) {
                    drawStrokeFromString(segments[i]);
                }
            } else {
                // Handle single segment stroke
                strokes.add(message);
                drawStrokeFromString(message);
            }
            repaint();
        }

        // FOCUS HERE: Fixed clear method
        public void clear() {
            if (g2 != null) {
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                strokes.clear();
                currentStrokeSegments.clear();
                repaint();
                System.out.println("Canvas cleared. Strokes: " + strokes.size());
            }
        }

        // FOCUS HERE: Fixed undo method
        public void undoLastStroke() {
            if (!strokes.isEmpty()) {
                strokes.remove(strokes.size() - 1);
                redrawAllStrokes();
                System.out.println("Undo successful. Remaining strokes: " + strokes.size());
            } else {
                System.out.println("Nothing to undo");
            }
        }

        // FOCUS HERE: Completely redraw everything after an undo
        private void redrawAllStrokes() {
            if (g2 == null) return;
            
            // Clear the canvas
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            // Redraw all remaining strokes
            for (String stroke : strokes) {
                if (stroke.startsWith("MULTISEGMENT")) {
                    String[] segments = stroke.split("\\|");
                    for (int i = 1; i < segments.length; i++) {
                        drawStrokeFromString(segments[i]);
                    }
                } else {
                    drawStrokeFromString(stroke);
                }
            }
            repaint();
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

        // Draw from a stored string format
        private void drawStrokeFromString(String message) {
            String[] parts = message.split(",");
            
            if (parts.length < 9) {
                System.out.println("Invalid stroke format: " + message);
                return;
            }

            try {
                String mode = parts[0];
                int weight = Integer.parseInt(parts[1]);
                Color color = new Color(
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4])
                );
                
                int x1 = Integer.parseInt(parts[5]);
                int y1 = Integer.parseInt(parts[6]);
                int x2 = Integer.parseInt(parts[7]);
                int y2 = Integer.parseInt(parts[8]);
                
                drawLine(x1, y1, x2, y2, mode, weight, color);
            } catch (Exception e) {
                System.err.println("Error parsing stroke: " + message);
                e.printStackTrace();
            }
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