package fr.jbe.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainMouseDraw extends Application {

    private Canvas mapCanvas;
    private Canvas gridCanvas;
    private Canvas cursorCanvas;
    private double width = 800;
    private double height = 600;
    private double cellSize = 64;
    private double verticalCellCount;
    private double horizontalCellCount;
    private Color gridColor = Color.BLACK;
    private GraphicsContext mapGraphicsContext;
    private GraphicsContext gridGraphicsContext;
    private GraphicsContext cursorGraphicsContext;
    private Map<Long,Image> resourcesMap = new HashMap<>();
    private Long imageIdSelected;
    private List<MapData> mapDatas;
    private int oldCol = -1;
    private int oldRow = -1;
    private Label nbImage = new Label("Nb images : 0");
    private Label selectedImage = new Label("Selected : none");
    private Label eraseLabel = new Label("Erase mode : false");
    private boolean eraseMode = false;
    private List<Line> sceneLines;
    private Canvas lineCanvas;
    private Canvas cursorLineCanvas;
    private GraphicsContext lineGraphicsContext;
    private GraphicsContext cursorLineGraphicsContext;
    private ToggleButton btEditMap;
    private boolean editMap = true;
    private ToggleButton btEditLine;
    private boolean editLine = false;
    private PVector lineStart;
    private PVector lineEnd;
    private boolean snapGrid = false;
    private ToggleButton btSnapGrid;
    private boolean poliLine = false;
    private ToggleButton btPoliLine;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        mapDatas = new ArrayList<>();
        sceneLines = new ArrayList<>();
        oldCol = -1;
        oldRow = -1;
        lineStart = null;
        lineEnd = null;
        
        Image image1 = new Image(this.getClass().getResourceAsStream("/ft_conc01_c_512.png"), 512, 512, true, true);
        resourcesMap.put(Long.valueOf(1), image1);
        Image image2 = new Image(this.getClass().getResourceAsStream("/ft_conc01_c_64.png"), 64, 64, true, true);
        resourcesMap.put(Long.valueOf(2), image2);
        Image image3 = new Image(this.getClass().getResourceAsStream("/ft_stone01_c_512.png"), 512, 512, true, true);
        resourcesMap.put(Long.valueOf(3), image3);
        Image image4 = new Image(this.getClass().getResourceAsStream("/ft_stone01_c_64.png"), 64, 64, true, true);
        resourcesMap.put(Long.valueOf(4), image4);
        
        BorderPane root = new BorderPane();

        StackPane stackLayout = new StackPane();
        root.setCenter(stackLayout);

        mapCanvas = new Canvas(width, height);
        mapGraphicsContext = mapCanvas.getGraphicsContext2D();
        stackLayout.getChildren().add(mapCanvas);

        lineCanvas = new Canvas(width, height);
        lineGraphicsContext = lineCanvas.getGraphicsContext2D();
        lineGraphicsContext.setStroke(Color.BLACK);
        lineGraphicsContext.setLineWidth(4);
        stackLayout.getChildren().add(lineCanvas);
        
        cursorLineCanvas = new Canvas(width, height);
        cursorLineGraphicsContext = cursorLineCanvas.getGraphicsContext2D();
        cursorLineGraphicsContext.setStroke(Color.BLACK);
        cursorLineGraphicsContext.setLineWidth(4);
        stackLayout.getChildren().add(cursorLineCanvas);

        cursorCanvas = new Canvas(width, height);
        cursorGraphicsContext = cursorCanvas.getGraphicsContext2D();
        stackLayout.getChildren().add(cursorCanvas);

        gridCanvas = new Canvas(width, height);
        gridGraphicsContext = gridCanvas.getGraphicsContext2D();
        stackLayout.getChildren().add(gridCanvas);
        stackLayout.setOnMouseMoved((MouseEvent event) -> {
            if (editMap) {
                higlightCell(event);
            }
            if (editLine && poliLine && lineStart != null) {
                lineEnd = traceLineToEnd(event.getX(), event.getY());
            }
        });
        stackLayout.setOnMouseReleased((MouseEvent event) -> {
            if (editMap) {
                if (eraseMode) {
                    eraseCell(event.getX(), event.getY());
                } else {
                    paintImage(event.getX(), event.getY());                
                }
            }
            if (editLine) {
                if (poliLine) {
                    if (lineStart == null) {
                        lineStart = getLineStart(event.getX(), event.getY());
                    } else if (lineEnd != null) {
                        MouseButton button = event.getButton();
                        if (button.equals(MouseButton.PRIMARY)) {
                            drawLine();
                            lineStart = lineEnd;
                        } else {
                            cursorLineGraphicsContext.clearRect(0, 0, width, height);
                            lineStart = null;
                        }
                        lineEnd = null;
                    }
                } else {
                    if (lineStart != null && lineEnd != null) {
                        drawLine();
                        lineStart = null;
                        lineEnd = null;
                    }
                }
            }
        });
        stackLayout.setOnMouseDragged((MouseEvent event) -> {
            if (editMap) {
                higlightCell(event);
                if (eraseMode) {
                    eraseCell(event.getX(), event.getY());
                } else {
                    int col = (int) (horizontalCellCount / width * event.getX());
                    int row = (int) (verticalCellCount / height * event.getY());
                    if (col != oldCol || row != oldRow) {
                        paintImage(event.getX(), event.getY());
                    }
                }
            }
            if (editLine && !poliLine && lineStart != null) {
                lineEnd = traceLineToEnd(event.getX(), event.getY());
            }
        });
        stackLayout.setOnMousePressed((MouseEvent event) -> {
            if (editLine && !poliLine) {
                lineStart = getLineStart(event.getX(), event.getY());
            }
        });

        paintGrid();
        
        VBox toolPane = new VBox();
        toolPane.setPrefHeight(600);
        toolPane.setPrefWidth(150);
        toolPane.setSpacing(10);
        toolPane.setPadding(new Insets(10));
        toolPane.getChildren().add(nbImage);
        toolPane.getChildren().add(eraseLabel);
        toolPane.getChildren().add(selectedImage);
        btEditMap = new ToggleButton("Edit Map");
        btEditMap.setOnAction((ActionEvent event) -> {
            editMap = btEditMap.isSelected();
            if (btEditMap.isSelected()) {
                editLine = false;
                btEditLine.setSelected(false);
            }
        });
        toolPane.getChildren().add(btEditMap);
        btEditMap.setSelected(true);
        btEditLine = new ToggleButton("Edit Line");
        btEditLine.setOnAction((ActionEvent event) -> {
            editLine = btEditLine.isSelected();
            if (btEditLine.isSelected()) {
                editMap = false;
                btEditMap.setSelected(false);
                cursorGraphicsContext.clearRect(0, 0, width, height);
            }
        });
        toolPane.getChildren().add(btEditLine);
        btSnapGrid = new ToggleButton("Snap Grid");
        btSnapGrid.setOnAction((ActionEvent event) -> {
            snapGrid = btSnapGrid.isSelected();
        });
        toolPane.getChildren().add(btSnapGrid);
        btPoliLine = new ToggleButton("Poli Line");
        btPoliLine.setOnAction((ActionEvent event) -> {
            poliLine = btPoliLine.isSelected();
        });
        toolPane.getChildren().add(btPoliLine);
        Button btImage1 = new Button();
        btImage1.setText("Image 1");
        btImage1.setOnMouseClicked((MouseEvent event) -> {
            selectImage(Long.valueOf(1));
        });
        toolPane.getChildren().add(btImage1);
        Button btImage2 = new Button();
        btImage2.setText("Image 2");
        btImage2.setOnMouseClicked((MouseEvent event) -> {
            selectImage(Long.valueOf(2));
        });
        toolPane.getChildren().add(btImage2);
        Button btImage3 = new Button();
        btImage3.setText("Image 3");
        btImage3.setOnMouseClicked((MouseEvent event) -> {
            selectImage(Long.valueOf(3));
        });
        toolPane.getChildren().add(btImage3);
        Button btImage4 = new Button();
        btImage4.setText("Image 4");
        btImage4.setOnMouseClicked((MouseEvent event) -> {
            selectImage(Long.valueOf(4));
        });
        toolPane.getChildren().add(btImage4);
        Button btErase = new Button();
        btErase.setText("Erase");
        btErase.setOnMouseClicked((MouseEvent event) -> {
            eraseMode = !eraseMode;
            eraseLabel.setText("Erase mode : "+eraseMode);
        });
        toolPane.getChildren().add(btErase);
        Button btReset = new Button();
        btReset.setText("Reset");
        btReset.setOnMouseClicked((MouseEvent event) -> {
            mapGraphicsContext.clearRect(0, 0, width, height);
            lineGraphicsContext.clearRect(0, 0, width, height);
        });
        toolPane.getChildren().add(btReset);
        Button btRepaint = new Button();
        btRepaint.setText("Repaint");
        btRepaint.setOnMouseClicked((MouseEvent event) -> {
            repaintMap();
            repaintLine();
        });
        toolPane.getChildren().add(btRepaint);
        
        root.setRight(toolPane);

        Scene scene = new Scene(root, 950, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private PVector getLineStart(double mouseX, double mouseY) {
        double x = mouseX;
        double y = mouseY;
        if (snapGrid) {
            x = getSnapedX(x);
            y = getSnapedY(y);
        }
        return new PVector(x, y);
    }
    
    private PVector traceLineToEnd(double mouseX, double mouseY) {
        cursorLineGraphicsContext.clearRect(0, 0, width, height);
        double x = mouseX;
        double y = mouseY;
        if (snapGrid) {
            x = getSnapedX(x);
            y = getSnapedY(y);
        }
        cursorLineGraphicsContext.strokeLine(lineStart.x, lineStart.y, x, y);
        return new PVector(x, y);
    }
    
    private void drawLine() {
        Line line = new Line(lineStart, lineEnd);
        sceneLines.add(line);
        lineGraphicsContext.strokeLine(lineStart.x, lineStart.y, lineEnd.x, lineEnd.y);
        cursorLineGraphicsContext.clearRect(0, 0, width, height);
    }

    private void repaintMap() {
        mapGraphicsContext.clearRect(0, 0, width, height);
        for (MapData mapData : mapDatas) {
            Image currentImage = resourcesMap.get(mapData.getId());
            if (currentImage != null) {                    
                mapGraphicsContext.drawImage(currentImage, mapData.getX(), mapData.getY());
            }
        }
    }
    
    private void repaintLine() {
        lineGraphicsContext.clearRect(0, 0, width, height);
        for (Line line : sceneLines) {
            lineGraphicsContext.strokeLine(line.getStart().x, line.getStart().y, line.getEnd().x, line.getEnd().y);
        }
    }

    private void higlightCell(MouseEvent event) {
        cursorGraphicsContext.clearRect(0, 0, width, height);
        Color highlightColor = Color.web("LIGHTBLUE", 0.5);

        int col = (int) (horizontalCellCount / width * event.getX());
        int row = (int) (verticalCellCount / height * event.getY());

        cursorGraphicsContext.setFill(highlightColor);
        if (imageIdSelected != null) {
            Image currentImage = resourcesMap.get(imageIdSelected);
            cursorGraphicsContext.fillRect(col * cellSize, row * cellSize, currentImage.getWidth(), currentImage.getHeight());
        } else {
            cursorGraphicsContext.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);
        }
    }
    
    private void selectImage(Long id) {
        imageIdSelected = id;
        selectedImage.setText("Selected : Image "+id);
    }

    private void paintGrid() {
        verticalCellCount = height / cellSize;
        horizontalCellCount = width / cellSize;

        gridGraphicsContext.setStroke(gridColor);
        gridGraphicsContext.setLineWidth(1);

        // horizontal grid lines
        for (double row = 0; row < height; row += cellSize) {
            double y = (int) row + 0.5;
            gridGraphicsContext.strokeLine(0, y, width, y);
        }

        // vertical grid lines
        for (double col = 0; col < width; col += cellSize) {
            double x = (int) col + 0.5;
            gridGraphicsContext.strokeLine(x, 0, x, height);
        }

    }
    
    private void paintImage(double mouseX, double mouseY) {
        if (imageIdSelected != null) {
            Image currentImage = resourcesMap.get(imageIdSelected);
            if (currentImage != null) {
                int col = (int) (horizontalCellCount / width * mouseX);
                int row = (int) (verticalCellCount / height * mouseY);
                mapGraphicsContext.drawImage(currentImage, col * cellSize, row * cellSize);
                MapData mapData = new MapData();
                mapData.setX(col * cellSize);
                mapData.setY(row * cellSize);
                mapData.setWidth(currentImage.getWidth());
                mapData.setHeight(currentImage.getHeight());
                mapData.setId(imageIdSelected);
                removeAlreadyPainted(mapData);
                mapDatas.add(mapData);
                oldCol = col;
                oldRow = row;
                nbImage.setText("Nb images : "+mapDatas.size());
            }
        }
    }
    
    private void eraseCell(double mouseX, double mouseY) {
        if (imageIdSelected != null) {
            Image currentImage = resourcesMap.get(imageIdSelected);
            int col = (int) (horizontalCellCount / width * mouseX);
            int row = (int) (verticalCellCount / height * mouseY);
            MapData mapData = new MapData();
            mapData.setX(col * cellSize);
            mapData.setY(row * cellSize);
            mapData.setWidth(currentImage.getWidth());
            mapData.setHeight(currentImage.getHeight());
            mapData.setId(imageIdSelected);
            removeAlreadyPainted(mapData);
            repaintMap();
        }
    }
    
    private boolean removeAlreadyPainted(MapData mapData) {
        boolean exist = false;
        for (MapData containedMapData : mapDatas) {
            if(containedMapData.getId() == mapData.getId()
                    && containedMapData.getX() == mapData.getX()
                    && containedMapData.getY() == mapData.getY()) {
                mapDatas.remove(containedMapData);
                exist = true;
                break;
            }
        }
        return exist;
    }
    
    private double getSnapedX(double mouseX) {
        double x = mouseX;
        int col = (int) (horizontalCellCount / width * mouseX);
        double leftX = col * cellSize;
        double rightX = leftX + cellSize;
        if ((x - leftX) < (rightX - x)) {
            x = leftX;
        } else {
            x = rightX;
        }
        return x;
    }
    
    private double getSnapedY(double mouseY) {
        double y = mouseY;
        int row = (int) (verticalCellCount / height * mouseY);
        double topY = row * cellSize;
        double bottomY = topY + cellSize;
        if ((y - topY) < (bottomY - y)) {
            y = topY;
        } else {
            y = bottomY;
        }
        return y;
    }
    
}
