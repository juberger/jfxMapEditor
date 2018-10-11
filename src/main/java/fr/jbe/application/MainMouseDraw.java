package fr.jbe.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
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
    private Map<Long, Image> resourcesMap = new HashMap<>();
    private Long imageIdSelected;
    private List<MapData> mapDatas;
    private int oldCol = -1;
    private int oldRow = -1;
    private Label nbImage = new Label("Nb images : 0");
    private Label nbLine = new Label("Nb lignes : 0");
    private Label selectedImage = new Label("Selected : none");
    private Label eraseLabel = new Label("Erase mode : false");
    private boolean eraseMode = false;
    private List<Line> sceneLines;
    private Canvas lineCanvas;
    private Canvas cursorLineCanvas;
    private GraphicsContext lineGraphicsContext;
    private GraphicsContext cursorLineGraphicsContext;
    private boolean editMap = true;
    private boolean editLine = false;
    private PVector lineStart;
    private PVector lineEnd;
    private boolean snapGrid = false;
    private boolean poliLine = false;
    private boolean showLight = false;
    private Algorithm algorithm;
    private Canvas lightCanvas;
    private GraphicsContext lightGraphicsContext;
    private boolean gradientLight = false;
    private boolean redPoints = true;
    private Group fogLayout;
    private int selectedLine = -1;
    private boolean selectLineMode = false;
    private double mouseX = 0.0;
    private double mouseY = 0.0;
    private Line grabedLine = null;

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
        algorithm = new AlgorithmCustom();

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

        lightCanvas = new Canvas(width, height);
        lightGraphicsContext = lightCanvas.getGraphicsContext2D();
        stackLayout.getChildren().add(lightCanvas);

        fogLayout = new Group();
        stackLayout.getChildren().add(fogLayout);

        cursorCanvas = new Canvas(width, height);
        cursorGraphicsContext = cursorCanvas.getGraphicsContext2D();
        stackLayout.getChildren().add(cursorCanvas);

        gridCanvas = new Canvas(width, height);
        gridGraphicsContext = gridCanvas.getGraphicsContext2D();
        stackLayout.getChildren().add(gridCanvas);
        stackLayout.setOnMouseMoved((MouseEvent event) -> {
            if (showLight) {
                paintLight(event.getX(), event.getY());
            } else if (selectLineMode) { 
                PVector mouseLocation = new PVector(event.getX(), event.getY());
                int lineIndex = algorithm.getIntersectLineIndex(mouseLocation, sceneLines);
                repaintLine(lineIndex, Color.ORANGE);
            } else {
                if (editMap) {
                    higlightCell(event);
                }
                if (editLine && poliLine && lineStart != null) {
                    lineEnd = traceLineToEnd(event.getX(), event.getY());
                }
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
            } else if (selectLineMode && event.getButton().equals(MouseButton.PRIMARY)) {
                PVector mouseLocation = new PVector(event.getX(), event.getY());
                if (grabedLine != null) {
                    sceneLines.add(grabedLine);
                    grabedLine = null;
                    cursorLineGraphicsContext.clearRect(0, 0, width, height);
                }
                selectedLine = algorithm.getIntersectLineIndex(mouseLocation, sceneLines);
                if (selectedLine > -1) {
                    repaintLine();
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
            } else if (selectLineMode && event.getButton().equals(MouseButton.PRIMARY)) {
                if (grabedLine != null) {
                    double moveX = event.getX() - mouseX;
                    double moveY = event.getY() - mouseY;
                    grabedLine.getStart().set((grabedLine.getStart().x + moveX), (grabedLine.getStart().y + moveY), 0);
                    grabedLine.getEnd().set((grabedLine.getEnd().x + moveX), (grabedLine.getEnd().y + moveY), 0);
                    cursorLineGraphicsContext.clearRect(0, 0, width, height);
                    cursorLineGraphicsContext.strokeLine(grabedLine.getStart().x, grabedLine.getStart().y, grabedLine.getEnd().x, grabedLine.getEnd().y);
                    mouseX = event.getX();
                    mouseY = event.getY();
                }
            }
        });
        stackLayout.setOnMousePressed((MouseEvent event) -> {
            if (editLine && !poliLine) {
                lineStart = getLineStart(event.getX(), event.getY());
            } else if (selectLineMode && event.getButton().equals(MouseButton.PRIMARY)) {
                PVector mouseLocation = new PVector(event.getX(), event.getY());
                selectedLine = algorithm.getIntersectLineIndex(mouseLocation, sceneLines);
                if (selectedLine > -1) {
                    mouseX = event.getX();
                    mouseY = event.getY();
                    grabedLine = sceneLines.get(selectedLine);
                    sceneLines.remove(selectedLine);
                    cursorLineGraphicsContext.clearRect(0, 0, width, height);
                    cursorLineGraphicsContext.strokeLine(grabedLine.getStart().x, grabedLine.getStart().y, grabedLine.getEnd().x, grabedLine.getEnd().y);
                }
            }
        });

        paintGrid();
        
        createGui(root);

        Scene scene = new Scene(root, 950, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        scene.setOnKeyReleased((KeyEvent event) -> {
            if (event.getCode() == KeyCode.Z && event.isControlDown()) {
                if (editMap && mapDatas.size() > 0) {
                    mapDatas.remove(mapDatas.size() - 1);
                    repaintMap();
                } else if (editLine && sceneLines.size() > 0) {
                    sceneLines.remove(sceneLines.size() - 1);
                    repaintLine();
                }
            }
        });
    }
    
    private void createGui(BorderPane root) {
        VBox toolPane = new VBox();
        toolPane.setPrefHeight(600);
        toolPane.setPrefWidth(150);
        toolPane.setSpacing(10);
        toolPane.setPadding(new Insets(10));
        
        toolPane.getChildren().add(nbImage);
        toolPane.getChildren().add(nbLine);
        toolPane.getChildren().add(eraseLabel);
        toolPane.getChildren().add(selectedImage);
        
        final ToggleButton btEditLine = new ToggleButton("Off");
        final ToggleButton btEditMap = new ToggleButton("On");
        final ToggleButton btSnapGrid = new ToggleButton("Off");
        final ToggleButton btPoliLine = new ToggleButton("Off");
        final ToggleButton btHideLine = new ToggleButton("Off");
        final ToggleButton btSelectLine = new ToggleButton("Off");
        final ToggleButton btShowLight = new ToggleButton("Off");
        final ToggleButton btGradientLight = new ToggleButton("Off");
        final ToggleButton btRedPoints = new ToggleButton("On");
        final Button btReset = new Button();
        final Button btRepaint = new Button();
        final Button btErase = new Button();
        
        toolPane.getChildren().add(new Separator(Orientation.HORIZONTAL));
        
        GridPane mapGrid = new GridPane();
        mapGrid.setHgap(5);
        mapGrid.setVgap(5);
        Label editMapLabel = new Label("Edit Map");
        mapGrid.add(editMapLabel, 0, 0);
        btEditMap.setOnAction((ActionEvent event) -> {
            editMap = btEditMap.isSelected();
            if (btEditMap.isSelected()) {
                editLine = false;
                btEditLine.setSelected(false);
            }
            btEditMap.setText(editMap==true?"On":"Off");
        });
        btEditMap.setSelected(true);
        mapGrid.add(btEditMap, 1, 0);
        Button btImage1 = new Button();
        btImage1.setText("Image 1");
        btImage1.setOnMouseClicked((MouseEvent event) -> {
            selectImage(Long.valueOf(1));
        });
        mapGrid.add(btImage1, 0, 1);
        Button btImage2 = new Button();
        btImage2.setText("Image 2");
        btImage2.setOnMouseClicked((MouseEvent event) -> {
            selectImage(Long.valueOf(2));
        });
        mapGrid.add(btImage2, 1, 1);
        Button btImage3 = new Button();
        btImage3.setText("Image 3");
        btImage3.setOnMouseClicked((MouseEvent event) -> {
            selectImage(Long.valueOf(3));
        });
        mapGrid.add(btImage3, 0, 2);
        Button btImage4 = new Button();
        btImage4.setText("Image 4");
        btImage4.setOnMouseClicked((MouseEvent event) -> {
            selectImage(Long.valueOf(4));
        });
        mapGrid.add(btImage4, 1, 2);
        toolPane.getChildren().add(mapGrid);

        toolPane.getChildren().add(new Separator(Orientation.HORIZONTAL));
        
        GridPane lineGrid = new GridPane();
        lineGrid.setHgap(5);
        lineGrid.setVgap(5);
        Label editLineLabel = new Label("Edit Line");
        lineGrid.add(editLineLabel, 0, 0);
        btEditLine.setOnAction((ActionEvent event) -> {
            editLine = btEditLine.isSelected();
            if (btEditLine.isSelected()) {
                editMap = false;
                btEditMap.setSelected(false);
                btSelectLine.setSelected(false);
                cursorGraphicsContext.clearRect(0, 0, width, height);
                btEditLine.setText(editLine==true?"On":"Off");
            }
        });
        lineGrid.add(btEditLine, 1, 0);
        Label snapGridLabel = new Label("Snap Grid");
        lineGrid.add(snapGridLabel, 0, 1);
        btSnapGrid.setOnAction((ActionEvent event) -> {
            snapGrid = btSnapGrid.isSelected();
            btSnapGrid.setText(snapGrid==true?"On":"Off");
        });
        lineGrid.add(btSnapGrid, 1, 1);
        Label polyLineLabel = new Label("Poly Line");
        lineGrid.add(polyLineLabel, 0, 2);
        btPoliLine.setOnAction((ActionEvent event) -> {
            poliLine = btPoliLine.isSelected();
            btPoliLine.setText(poliLine==true?"On":"Off");
        });
        lineGrid.add(btPoliLine, 1, 2);
        Label hideLineLabel = new Label("Hide Line");
        lineGrid.add(hideLineLabel, 0, 3);
        btHideLine.setOnAction((ActionEvent event) -> {
            lineCanvas.setVisible(!btHideLine.isSelected());
            btHideLine.setText(btHideLine.isSelected()==true?"On":"Off");
        });
        lineGrid.add(btHideLine, 1, 3);
        Label selectLineLabel = new Label("Select Line");
        lineGrid.add(selectLineLabel, 0, 4);
        btSelectLine.setOnAction((ActionEvent event) -> {
            selectLineMode = btSelectLine.isSelected();
            if (selectLineMode) {
                btEditLine.setSelected(false);
                editLine = false;
            }
            btSelectLine.setText(btSelectLine.isSelected()==true?"On":"Off");
        });
        lineGrid.add(btSelectLine, 1, 4);
        toolPane.getChildren().add(lineGrid);

        toolPane.getChildren().add(new Separator(Orientation.HORIZONTAL));
        
        GridPane lightGrid = new GridPane();
        lightGrid.setHgap(5);
        lightGrid.setVgap(5);
        Label showLightLabel = new Label("Show Light");
        lightGrid.add(showLightLabel, 0, 0);
        btShowLight.setOnAction((ActionEvent event) -> {
            showLight = btShowLight.isSelected();
            if (!showLight) {
                lightGraphicsContext.clearRect(0, 0, width, height);
                fogLayout.getChildren().clear();
            }
            btShowLight.setText(showLight==true?"On":"Off");
        });
        lightGrid.add(btShowLight, 1, 0);
        Label gradLightLabel = new Label("Gradient Light");
        lightGrid.add(gradLightLabel, 0, 1);
        btGradientLight.setOnAction((ActionEvent event) -> {
            gradientLight = btGradientLight.isSelected();
            btGradientLight.setText(gradientLight==true?"On":"Off");
        });
        lightGrid.add(btGradientLight, 1, 1);
        Label redPointLabel = new Label("Red Points");
        lightGrid.add(redPointLabel, 0, 2);
        btRedPoints.setOnAction((ActionEvent event) -> {
            redPoints = btRedPoints.isSelected();
            btRedPoints.setText(redPoints==true?"On":"Off");
        });
        btRedPoints.setSelected(true);
        lightGrid.add(btRedPoints, 1, 2);
        toolPane.getChildren().add(lightGrid);

        toolPane.getChildren().add(new Separator(Orientation.HORIZONTAL));
        
        GridPane resetGrid = new GridPane();
        resetGrid.setHgap(5);
        resetGrid.setVgap(5);
        btReset.setText("Reset");
        btReset.setOnMouseClicked((MouseEvent event) -> {
            mapGraphicsContext.clearRect(0, 0, width, height);
            lineGraphicsContext.clearRect(0, 0, width, height);
        });
        resetGrid.add(btReset, 0, 0);
        btRepaint.setText("Repaint");
        btRepaint.setOnMouseClicked((MouseEvent event) -> {
            repaintMap();
            repaintLine();
        });
        resetGrid.add(btRepaint, 1, 0);
        
        btErase.setText("Erase");
        btErase.setOnMouseClicked((MouseEvent event) -> {
            eraseMode = !eraseMode;
            eraseLabel.setText("Erase mode : " + eraseMode);
        });
        resetGrid.add(btErase, 0, 1);
        toolPane.getChildren().add(resetGrid);
        
        root.setRight(toolPane);
    }

    private void paintLight(double x, double y) {
        cursorGraphicsContext.clearRect(0, 0, width, height);
        lightGraphicsContext.clearRect(0, 0, width, height);

        // scanlines
        List<Line> scanLines = algorithm.createScanLines(x, y);
        // get intersection points
        List<PVector> points = algorithm.getIntersectionPoints(scanLines, sceneLines);

        // draw intersection shape
        lightGraphicsContext.setStroke(Color.GREEN);

        if (gradientLight) {

            Color LIGHT_GRADIENT_START = Color.YELLOW.deriveColor(1, 1, 1, 0.5);
            Color LIGHT_GRADIENT_END = Color.TRANSPARENT;

            // TODO: don't use the center of the shape; instead calculate the center depending on the user position
            RadialGradient gradient = new RadialGradient(0,
                                                         0,
                                                         0.5,
                                                         0.5,
                                                         0.5,
                                                         true,
                                                         CycleMethod.NO_CYCLE,
                                                         new Stop(0, LIGHT_GRADIENT_START),
                                                         new Stop(1, LIGHT_GRADIENT_END));
            lightGraphicsContext.setFill(gradient);

            lightGraphicsContext.setFill(gradient);

        } else {

            lightGraphicsContext.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.7));

        }

        int count = 0;
        lightGraphicsContext.beginPath();
        for (PVector point : points) {
            if (count == 0) {
                lightGraphicsContext.moveTo(point.x, point.y);
            } else {
                lightGraphicsContext.lineTo(point.x, point.y);
            }
            count++;
        }
        lightGraphicsContext.closePath();

        // stroke
        // if( Settings.get().isShapeBorderVisible()) {
        lightGraphicsContext.stroke();
        // }

        // fill
        lightGraphicsContext.fill();

        // draw intersection points
        if (redPoints) {

            lightGraphicsContext.setStroke(Color.RED);
            lightGraphicsContext.setFill(Color.RED.deriveColor(1, 1, 1, 0.5));

            double w = 2;
            double h = w;
            for (PVector point : points) {
                lightGraphicsContext.strokeOval(point.x - w / 2, point.y - h / 2, w, h);
                lightGraphicsContext.fillOval(point.x - w / 2, point.y - h / 2, w, h);
            }

        }

        Rectangle fog = new Rectangle(width, height);
        fog.relocate(0, 0);

        Polygon polyLight = getPolygonByPoints(points);
        Shape fogPath = Path.subtract(fog, polyLight);
        fogPath.setFill(Color.BLACK.deriveColor(1, 1, 1, 0.9));
        fogLayout.getChildren().clear();
        fogLayout.getChildren().add(fogPath);
    }

    private Polygon getPolygonByPoints(List<PVector> points) {
        double[] poliXY = new double[(points.size()*2)];
        int index = 0;
        for (PVector point : points) {
            poliXY[index] = point.x;
            index++;
            poliXY[index] = point.y;
            index++;
        }
        
        return new Polygon(poliXY);
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
        nbLine.setText("Nb lignes : "+sceneLines.size());
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
        repaintLine(-1, null);
    }
    
    private void repaintLine(int lineIndex, Color lineColor) {
        lineGraphicsContext.clearRect(0, 0, width, height);
        for (int index = 0; index < sceneLines.size(); index++) {
            Line line = sceneLines.get(index);
            if (selectedLine > -1 && index == selectedLine) {
                lineGraphicsContext.setStroke(Color.RED);
            } else if (lineIndex > -1 && lineIndex == index) {
                if (lineColor != null) {
                    lineGraphicsContext.setStroke(lineColor);
                } else {
                    lineGraphicsContext.setStroke(Color.RED);
                }
            } else {
                lineGraphicsContext.setStroke(Color.BLACK);
            }
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
            cursorGraphicsContext.fillRect(col * cellSize,
                                           row * cellSize,
                                           currentImage.getWidth(),
                                           currentImage.getHeight());
        } else {
            cursorGraphicsContext.fillRect(col * cellSize, row * cellSize, cellSize, cellSize);
        }
    }

    private void selectImage(Long id) {
        imageIdSelected = id;
        selectedImage.setText("Selected : Image " + id);
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
                nbImage.setText("Nb images : " + mapDatas.size());
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
            if (containedMapData.getId() == mapData.getId() && containedMapData.getX() == mapData.getX()
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
