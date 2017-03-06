// DoodleView.java
// Main View for the Doodlz app.
package com.deitel.doodlz;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.print.PrintHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


public class DoodleView extends View {

    private static final float TOUCH_TOLERANCE = 10;

    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private final Paint paintScreen;
    private final Paint paintLine;
    private final Paint paintEraser, paintSelected, paintSelectedTwo, paintBorder;
    private Paint paintText;
    private static final String TAG = "Touch";
    private String selected_tool, prev_tool, input_text;
    private Point point1, point2;
    private PointF start, mid;
    private Integer left, right, top, bottom;
    private float  xc, yc, radius;
    private MyPath dPath, drawPath,xPath ;
    private Path screenPath;
    private RectF rectf, rectf0;
    private Stack <MyPath> pathStack, backStack;
    private Matrix matrix, matrixCanvas, matrixSaved,matrixStart;
    private int Stacksize;
    private boolean isLine,isEdit,isScale,isShift,isStackEmpty;

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int  ZOOM = 2;

    int mode = NONE;
    float oldDist = 1f;
    private  float[] v= new float[9];

    private final Map<Integer, MyPath> pathMap = new HashMap<>();
    private final Map<Integer, Point> previousPointMap =  new HashMap<>();


    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintScreen = new Paint();


        paintLine = new Paint();
        paintEraser = new Paint();
        paintSelected = new Paint();
        paintSelectedTwo = new Paint();
        paintBorder = new Paint();
        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);

        point1 = new Point();
        point2 = new Point();
        dPath = new MyPath();
        drawPath = new MyPath();
        rectf = new RectF();
        rectf0 = new RectF();
        xPath = new MyPath();
        screenPath = new Path();
        pathStack  = new Stack<MyPath>();
        backStack = new Stack <MyPath>();
        matrix = new Matrix();
        matrixCanvas = new Matrix();
        matrixSaved = new Matrix();
        matrixStart = new Matrix();
        start = new PointF();
        mid = new PointF();

        Stacksize= 20;
        left = right = top =  bottom = 0;
        xc= yc= radius=0;

        isLine = isEdit = isScale = isShift = isStackEmpty= false;      //

        selected_tool = "pencil";
        prev_tool = "";
        input_text = "";

        paintLine.setAntiAlias(true);
        paintLine.setColor(Color.BLACK);

        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(5);
        paintLine.setStrokeCap(Paint.Cap.ROUND);

        paintEraser.setColor(Color.WHITE);
        paintEraser.setStyle(Paint.Style.STROKE);
        paintEraser.setStrokeCap(Paint.Cap.ROUND);
        paintEraser.setStrokeWidth(50);

        paintSelected.set(paintLine);
        paintSelected.setColor(getResources().getColor(R.color.colorSelect));
        paintSelected.setStyle(Paint.Style.FILL_AND_STROKE);

        paintSelectedTwo.set(paintLine);

        paintBorder.setColor(Color.BLACK);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(2);

    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {

        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

        Log.d("height",String.valueOf(getHeight()));
        Log.d("width",String.valueOf(getWidth()));

        bitmapCanvas = new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE);
        matrixSaved.reset();
        matrixCanvas.reset();
        RectF rect_f = new RectF(0,0,getWidth(),getHeight());
        screenPath.reset();
        screenPath.addRect(rect_f, Path.Direction.CW);
    }


    public void clear() {
        pathStack.clear();
        backStack.clear();
        dPath.reset();
        drawPath.reset();
        xPath.reset();
        if(selected_tool.equals("edit")) selected_tool="rectangle";
        rectf.setEmpty();
        pathMap.clear();
        previousPointMap.clear();
        bitmap.eraseColor(Color.WHITE);
        isLine = false;
        matrix.reset();
        matrixSaved.reset();
        matrixCanvas.reset();
        isScale = false;
        bitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(),  true);
        bitmapCanvas = new Canvas(bitmap);
        invalidate();
    }


    public void setDrawingColor(int color) {
        paintLine.setColor(color);
        paintText.setColor(color);
    }

    public void setInputString(String text, String size, Boolean isBold, Boolean isItalic, Boolean LineType){
        input_text = text;
        paintText.setTypeface(Typeface.DEFAULT);
        if(isBold) paintText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        if(isItalic) paintText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        if(isBold & isItalic)  paintText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));

        if(LineType) paintText.setStyle(Paint.Style.FILL_AND_STROKE);
        else paintText.setStyle(Paint.Style.STROKE);
        paintText.setTextSize((float) Integer.valueOf(size));
        paintText.setStrokeWidth(5);
        Log.d("size", size);
        Log.d("bold", String.valueOf(isBold));
        Log.d("italic", String.valueOf(isItalic));
        Log.d("type", String.valueOf(LineType));
    }

    public void setImageBitmap(Bitmap bmp){
        Log.d("bitmap 1", "+");
        bitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
        bitmapCanvas = new Canvas(bitmap);

    }

    public int getDrawingColor() {
        return paintLine.getColor();
    }

    public void setLineWidth(int width) {
        paintLine.setStrokeWidth(width);
        paintEraser.setStrokeWidth(width);
    }

    public int getLineWidth() {
        return (int) paintLine.getStrokeWidth();
    }

    public void setSelectedTool(int tool){

        switch (tool){
            case 0://pencil
                selected_tool = "pencil";
                paintLine.setStyle(Paint.Style.STROKE);
                break;
            case 1://line
                selected_tool = "line";
                paintLine.setStyle(Paint.Style.STROKE);
                break;
            case 2://line
                selected_tool = "poly_line";
                paintLine.setStyle(Paint.Style.STROKE);
                break;
            case 3://line
                selected_tool = "eraser";
                paintLine.setStyle(Paint.Style.STROKE);
                break;
            case 4://circle
                selected_tool = "circle";
                paintLine.setStyle(Paint.Style.STROKE);
                break;
            case 5://circle fill
                selected_tool = "circle_fill";
                paintLine.setStyle(Paint.Style.FILL);
                break;
            case 6://oval
                selected_tool = "oval";
                paintLine.setStyle(Paint.Style.STROKE);
                break;
            case 7://oval fill
                selected_tool = "oval_fill";
                paintLine.setStyle(Paint.Style.FILL);
                break;
            case 8://square
                selected_tool = "square";
                paintLine.setStyle(Paint.Style.STROKE);
                break;
            case 9://square fill
                selected_tool = "square_fill";
                paintLine.setStyle(Paint.Style.FILL);
                break;
            case 10://rectangle
                selected_tool = "rectangle";
                paintLine.setStyle(Paint.Style.STROKE);
                break;
            case 11://rect fill
                selected_tool = "rectangle_fill";
                paintLine.setStyle(Paint.Style.FILL);
                break;
            case 12://editmode
                selected_tool = "edit";
                break;
            case 13:
                selected_tool = "copy";
                break;
            case 14:
                selected_tool = "text";
                break;
            case 15://handmode
                selected_tool = "hand";
                drawPathStack(bitmapCanvas,pathStack);
                clearStacks();
                break;


        }
        Log.d("Выбор меню", String.valueOf(tool));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Path newpath =new Path();
        screenPath.transform(matrixCanvas,newpath);

        canvas.clipPath(newpath);

        if(selected_tool.equals("hand")|| isScale|| !matrixCanvas.equals(matrixStart))
            canvas.drawBitmap(bitmap,matrixCanvas,paintScreen);

        else  canvas.drawBitmap(bitmap, 0, 0, paintScreen);

        if (!pathStack.empty()) drawPathStackCanvas(canvas,pathStack);

        switch (selected_tool) {
            case "pencil":
                for (Integer key : pathMap.keySet())
                    canvas.drawPath(pathMap.get(key), paintLine); // draw line
                break;
            case "eraser":
                for (Integer key : pathMap.keySet())
                    canvas.drawPath(pathMap.get(key), paintEraser); // draw line
                break;
            case "line":
            case "rectangle":
            case "rectangle_fill":
            case "oval":
            case "oval_fill":
            case "square":
            case "square_fill":
            case "circle":
            case "circle_fill":
            case "poly_line":
            case "edit":
            case "text":
            case "copy":
                Log.d("dPath", "DRAW на экран");
                canvas.drawPath(xPath, xPath.paint);
                break;
        }
    }

    private void drawPathStack(Canvas canvas,Stack <MyPath> stack) { // Рисование на битмар
        Stack rstack = (Stack) stack.clone();
        overStack(rstack);
        MyPath path;
        int i=0;
        while (!rstack.empty()) {
            path = (MyPath) rstack.pop();
            canvas.drawPath(path.getTransformPath(),path.paint);
            i++;
        }
        Log.d("DRAWStack", "B Bitmap "+  String.valueOf(i)+ "  элементов из стека");
        if(stack.empty()) Log.d("DRAWStack", "Cтек пуст");
    }

    private void drawPathStackCanvas(Canvas canvas,Stack <MyPath> stack) {
        Stack rstack = (Stack) stack.clone();
        overStack(rstack);
        MyPath path;
        int i=0;
        while (!rstack.empty()) {
            path= (MyPath) rstack.pop();

            if(!matrixCanvas.equals(path.matrix)) {
                Log.d("drawPathStackCanvas", "tranform getTransformPath()");
                Path tPath = new Path();
                path.getTransformPath();
                path.transform(matrixCanvas, tPath);
                canvas.drawPath(tPath,path.paint);
            }
            else  canvas.drawPath(path, path.paint);
            i++;
        }
        Log.d("DRAWStack", " на Экран"+  String.valueOf(i)+ "   элементов из стека");
        if(stack.empty()) Log.d("DRAWStack", "Cтек пуст");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (selected_tool) {
            case "eraser":
            case "pencil":
                int action = event.getActionMasked();
                int actionIndex = event.getActionIndex();

                if (action == MotionEvent.ACTION_DOWN ||
                        action == MotionEvent.ACTION_POINTER_DOWN) {
                    touchStarted(event.getX(actionIndex), event.getY(actionIndex),
                            event.getPointerId(actionIndex));
                } else if (action == MotionEvent.ACTION_UP ||
                        action == MotionEvent.ACTION_POINTER_UP) {
                    touchEnded(event.getPointerId(actionIndex));
                } else {
                    touchMoved(event);
                }

                break;
            case "poly_line":
            case "line":
            case "rectangle":
            case "rectangle_fill":
            case "oval":
            case "oval_fill":
            case "square":
            case "square_fill":
            case "circle":
            case "circle_fill":
            case "text":
                drawFigure(event);
                break;
            case "edit":
            case "copy":
                editMode(event);
                break;
            case "hand":
                viewMode(event);
                break;
        }
        invalidate(); // redraw
        return true;

    }

    private void drawFigure(MotionEvent event){
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        if(actionIndex==0)
            switch (action) {

                case MotionEvent.ACTION_DOWN:
                    drawFigureStart(event.getX(actionIndex), event.getY(actionIndex));
                    break;

                case MotionEvent.ACTION_MOVE:
                    drawFigureMove(event.getX(actionIndex), event.getY(actionIndex));
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    touchEnded(0);
                    break;
            }
    }

    private void drawFigureStart(float x, float y) {
        if(rectf.contains( x, y)& !selected_tool.equals("edit")&prev_tool.equals(selected_tool) ){
            point1.x = (int) x;
            point1.y = (int) y;
            isEdit = true;
            if(!pathStack.empty())
                drawPath = pathStack.pop();
        }
        else {
            switch (selected_tool) {
                case "poly_line":
                    if (isLine) {
                        point2.x = (int) x;
                        point2.y = (int) y;
                    }
                    else {
                        point1.x = (int) x;
                        point1.y = (int) y;
                    }
                    break;
                case "line":
                case "rectangle":
                case "rectangle_fill":
                case "oval":
                case "oval_fill":
                case "square":
                case "square_fill":
                case "circle":
                case "circle_fill":
                    dPath.reset();
                    point1.x = (int) x;
                    point1.y = (int) y;
                    isLine = false;                //
                    break;
                case "text":
                    dPath.reset();
                    setTextPath(x,y);

                    break;
            }
        }
    }

    private void moveFigure(float x, float y){

        float dx = x - point1.x;
        float dy = y - point1.y;

        Log.d("Edit", "Включен");
        matrix.reset();
        matrix.setTranslate(dx, dy);
        rectf.set(rectf0);
        rectf.offset(dx, dy);

        dPath.set(drawPath);
        dPath.transform(matrix);
        dPath.setRectF(rectf);
        xPath.set(dPath);
        xPath.setPaint(paintSelected);

    }

    private void drawFigureMove(float x, float y) {

        if (isEdit) moveFigure(x,y);
        else {

            switch (selected_tool) {
                case "line":
                case "poly_line":
                case "rectangle":
                case "rectangle_fill":
                case "oval":
                case "oval_fill":
                    point2.x = (int) x;
                    point2.y = (int) y;

                    if (point1.x < point2.x) {
                        left = point1.x;
                        right = point2.x;
                    } else {
                        right = point1.x;
                        left = point2.x;
                    }

                    if (point1.y > point2.y) {
                        bottom = point1.y;
                        top = point2.y;
                    } else {
                        top = point1.y;
                        bottom = point2.y;
                    }
                    xc = (left + right) / 2;
                    yc = (bottom + top) / 2;
                    dPath.reset();
                    addNewPath();

                    break;

                case "square":
                case "square_fill":
                case "circle":
                case "circle_fill":

                    point2.x = (int) x;
                    point2.y = (int) y;

                    int dx = Math.abs(point2.x - point1.x);
                    int dy = Math.abs(point2.y - point1.y);

                    if (dx > dy)
                        radius = dy / 1.4f;
                    else radius = dx / 1.4f;

                    if (point1.x < point2.x)
                        xc = point1.x + radius;
                    else
                        xc = point1.x - radius;
                    if (point1.y < point2.y)
                        yc = point1.y + radius;
                    else
                        yc = point1.y - radius;


                    dPath.reset();
                    addNewPath();
                    break;
                case "text":
                    setTextPath(x,y);
                    break;

            }
        }
    }

    public void setTextPath(float x,float y){
        Rect rect = new Rect();
        if(input_text.length()==0) return;
        paintText.getTextPath(input_text,0,input_text.length(),x,y,dPath);
        paintText.getTextBounds(input_text,0,input_text.length(),rect);
        rectf.set(rect);
        rectf.offset(x,y);
        dPath.setPaint(paintText);
        dPath.setRectF(rectf);
        dPath.setText(input_text);
        dPath.setMatrix(matrixCanvas);
        xPath.reset();
        xPath.addPath(dPath);
        xPath.setPaint(paintText);

    }

    private void editMode(MotionEvent event) {

        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        if (actionIndex == 0) {
            float x = event.getX(actionIndex);
            float y = event.getY(actionIndex);

            switch (action) {

                case MotionEvent.ACTION_DOWN:

                    point1.x = (int) x;  //
                    point1.y = (int) y;

                    if (pathStack.empty()||!pathStack.peek().rotable()) {
                        isStackEmpty=true;
                        return;
                    }
                    else {
                        isStackEmpty=false;

                        if (selected_tool == "edit") drawPath = pathStack.pop();
                        else drawPath = pathStack.peek();
                    }
                    if (rectf.contains(x, y))

                        isShift = true;

                    else {  // Режим вращения
                        xc = getWidth() / 2;
                        yc = getHeight() / 2;
                        isShift = false;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:

                    if(isStackEmpty) return;

                    xc = rectf.centerX();
                    yc = rectf.centerY();

                    if (isShift) moveFigure(x, y);

                    else  {

                        float dx = point1.x - xc;
                        float dy = event.getY(0) - point1.y;
                        float xy = event.getX(0) - point1.x;
                        dy -= xy;
                        Log.d("Rotation", "Включен");
                        float deg = (float) Math.atan(dy / dx) * 360 / (float) Math.PI;
                        matrix.reset();
                        matrix.setRotate(deg, xc, yc);
                        dPath.reset();
                        xPath.reset();
                        dPath.addPath(drawPath);
                        xPath.addRect(xc - 30, yc - 1, xc + 30, yc + 1, Path.Direction.CW);
                        xPath.addRect(xc - 1, yc - 30, xc + 1, yc + 30, Path.Direction.CW);
                        dPath.setPaint(paintLine);
                        xPath.setPaint(paintSelectedTwo);
                        dPath.transform(matrix);
                        xPath.addPath(dPath);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:

                    if(isStackEmpty) return;
                    touchEnded(0);
                    break;
            }
        }
    }

    private void viewMode(MotionEvent event){

        int action = event.getActionMasked();


        switch (action) {
            case MotionEvent.ACTION_DOWN:
                matrixSaved.set(matrixCanvas);
                start.set(event.getX(), event.getY());
                Log.d(TAG, "mode=DRAG");
                mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                Log.d(TAG, "oldDist=" + oldDist);
                if (oldDist > 10f) {
                    midPoint(mid, event);
                    mode = ZOOM;
                    Log.d(TAG, "mode=ZOOM");

                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Log.d(TAG, "ACTION _POINTER_UP");

                matrixCanvas.getValues(v);
                Log.d(TAG, "Matrix_scaling=" + v[0]);

                if(v[0]>16 ||  v[0] <0.2f)
                {
                    matrixCanvas.set(matrixSaved);
                    matrixCanvas.getValues(v);
                    Log.d(TAG, "New_matrix_scaling=" + v[0]);
                }

            case MotionEvent.ACTION_UP:
                mode = NONE;
                Log.d(TAG, "mode=NONE");
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    // ...
                    matrixCanvas.set(matrixSaved);
                    matrixCanvas.postTranslate(event.getX() - start.x,
                            event.getY() - start.y);
                }
                else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    Log.d(TAG, "newDist=" + newDist);
                    if (newDist > 10f) {
                        matrixCanvas.set(matrixSaved);
                        float scale = newDist / oldDist;
                        Log.d(TAG, "Scale=" + scale);
                        matrixCanvas.postScale(scale, scale, mid.x, mid.y);

                    }
                }
                break;
        }

        invalidate();
    }




    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }


    private void touchStarted(float x, float y, int lineID) {

        MyPath path;
        Point point;
        Log.d("touchstart","START");
        Log.d("isLine", String.valueOf(isLine));
        Log.d("TOOL", selected_tool);


        if (pathMap.containsKey(lineID)) {
            path = pathMap.get(lineID);
            path.reset();
            point = previousPointMap.get(lineID);
        } else {
            path = new MyPath();
            pathMap.put(lineID, path);
            point = new Point();
            previousPointMap.put(lineID, point);
        }


        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;
        isLine = false;

    }

    private void touchMoved(MotionEvent event) {



        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerID = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerID);

            if (pathMap.containsKey(pointerID)) {
                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);
                MyPath path = pathMap.get(pointerID);
                Point point = previousPointMap.get(pointerID);

                float deltaX = Math.abs(newX - point.x);
                float deltaY = Math.abs(newY - point.y);

                if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE) {
                    path.quadTo(point.x, point.y, (newX + point.x) / 2,
                            (newY + point.y) / 2);

                    if (selected_tool.equals("pencil"))
                        path.setPaint(paintLine);
                    else path.setPaint(paintEraser);
                    path.setMatrix(matrixCanvas);
                    point.x = (int) newX;
                    point.y = (int) newY;
                }
            }
        }
    }

    private void addNewPath(){

        switch (selected_tool) {
            case "line":
            case "poly_line":
                dPath.moveTo(point1.x, point1.y);
                dPath.lineTo(point2.x, point2.y);

                radius = 100;
                rectf.set(xc-radius, yc-radius, xc+radius, yc+radius);
                Log.d("center", String.valueOf(xc)+ "   " +String.valueOf(yc));
                break;

            case "rectangle":
            case "rectangle_fill":
                rectf.set(left, top, right, bottom);
                dPath.addRect(rectf,Path.Direction.CW );
                break;
            case "oval":
            case "oval_fill":
                rectf.set(left, top, right, bottom);
                dPath.addOval(rectf,Path.Direction.CW );
                break;
            case "square":
            case "square_fill":
                rectf.set(xc-radius, yc-radius, xc+radius, yc+radius);
                dPath.addRect(rectf,Path.Direction.CW );
                break;
            case "circle":
            case "circle_fill":
                rectf.set(xc-radius, yc-radius, xc+radius, yc+radius);
                dPath.addCircle(xc, yc, radius,Path.Direction.CW);
                break;
        }

        dPath.setPaint(paintLine);
        dPath.setRectF(rectf);
        dPath.setMatrix(matrixCanvas);
        xPath.reset();
        xPath.addPath(dPath);
        xPath.setPaint(paintLine);
    }

    private void touchEnded(int lineID) {
        Log.d("touchend","END");
        Log.d("maxStackSize",String.valueOf(Stacksize));
        Log.d("StackSize",String.valueOf(pathStack.size()));
        Log.d("LineID",String.valueOf(lineID));

        if (pathStack.size()>=Stacksize) {

            MyPath path= new MyPath();
            if(overStack(pathStack)){ path= (MyPath) pathStack.pop();
                Log.d("Push","P вышел из стека");}
            else  Log.d("Overstek"," Стек пуст");

            overStack(pathStack);
            bitmapCanvas.drawPath(path.getTransformPath(),path.paint);
        }

        switch (selected_tool) {
            case "pencil":
                MyPath path = pathMap.get(lineID);
                MyPath npath=new MyPath(path);
                npath.rectf.setEmpty();
                npath.setInrotable();
                pathStack.push(npath);
                path.setPaint(paintLine);

                Log.d("Push","P1 зашел в стек");
                path.reset();
                break;
            case "eraser":
                path = pathMap.get(lineID);
                npath=new MyPath(path);
                npath.setInrotable();
                npath.rectf.setEmpty();
                path.setPaint(paintEraser);
                pathStack.push(npath);
                Log.d("Push","P1 зашел в стек");
                path.reset();
                break;

            case "poly_line":
                isLine=true;
                point1.x=point2.x;
                point1.y=point2.y;
            case "line":
            case "rectangle":
            case "rectangle_fill":
            case "oval":
            case "oval_fill":
            case "square":
            case "square_fill":
            case "circle":
            case "circle_fill":
            case "edit":
            case "text":
            case "copy":

                npath = new MyPath(dPath);
                pathStack.push(npath);
                Log.d("Push","P2 зашел в стек");
                xPath.reset();
                rectf0.set(rectf);

                break;
        }

        isEdit = false;
        prev_tool = selected_tool;

    }

    private boolean overStack (Stack <MyPath> stack){

        Stack rstack= (Stack) stack.clone();
        stack.clear();
        int i=0;
        while (!rstack.empty()){
            stack.push((MyPath)rstack.pop()); i++;
        }
        Log.d("Stack","переворот стека");
        Log.d("Stack",String.valueOf(i)+" эл");
        return !stack.empty();

    }

    public void saveImage() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String userValue = prefs.getString("listPref","jpg");

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), System.currentTimeMillis()+"."+userValue);

        drawPathStack(bitmapCanvas,pathStack);

        try {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                switch (userValue){
                    case "png": bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        break;
                    case "jpg": bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        break;
                }
                addImageGallery(file);

            } finally {
                Toast message = Toast.makeText(getContext(), R.string.message_saved, Toast.LENGTH_SHORT);
                message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                        message.getYOffset() / 2);
                message.show();
                if (fos != null) fos.close();
            }
        } catch (Exception e) {

            Toast message = Toast.makeText(getContext(),
                    R.string.message_error_saving, Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                    message.getYOffset() / 2);
            message.show();

            e.printStackTrace();
        }

    }

    private void addImageGallery(File file){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
    }

    // print the current image
    public void printImage() {
        drawPathStack(bitmapCanvas,pathStack);
        if (PrintHelper.systemSupportsPrint()) {
            PrintHelper printHelper = new PrintHelper(getContext());

            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("Doodlz Image", bitmap);
        }
        else {
            Toast message = Toast.makeText(getContext(),
                    R.string.message_error_printing, Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                    message.getYOffset() / 2);
            message.show();
        }
    }

    public void undoDrawing(){
        if(!pathStack.empty())
        {
            backStack.push(pathStack.pop());
            dPath.reset();
            if(!pathStack.empty()) {
                rectf.set(pathStack.peek().rectf);

            } else rectf.setEmpty();
            invalidate();
        }
    }
    public void redoDrawing(){
        if(!backStack.empty())
        {
            pathStack.push(backStack.pop());
            rectf.set(pathStack.peek().rectf);
            invalidate();
        }
    }

    public void clearStacks(){
        drawPathStack(bitmapCanvas,pathStack);
        pathStack.clear();
        backStack.clear();
    }


}
