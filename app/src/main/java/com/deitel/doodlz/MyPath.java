package com.deitel.doodlz;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class MyPath extends Path {

    public RectF rectf;
    public Paint paint;
    public Matrix matrix;
    public String text;

    private boolean isText,isRotable;

    MyPath(MyPath Path) {
        super(Path);
        paint = new Paint();
        paint.set(Path.paint);
        rectf = new RectF(Path.rectf);
        isRotable = Path.isRotable;
        matrix = new Matrix(Path.matrix);
        text = Path.text;
        isText = Path.isText;
    }

    MyPath() {
        super();
        paint = new Paint();
        paint.reset();
        rectf = new RectF();
        isRotable = true;
        matrix = new Matrix();
        text="";
        isText= false;
    }

    public void setText(String st) {
        text=st;
        if (st.length()!=0)
        isText=true;
    }

    public boolean isText (){
        return isText;
    }

    public void setInrotable(){
        isRotable=false;
    }

    public boolean rotable(){
        return isRotable;
    }

    public void setPaint(Paint p) {
        paint.set(p);
    }

    public void setRectF(RectF rectF) {

        rectf.set(rectF);
    }

    public void setMatrix(Matrix newmatrix) {
        matrix.set(newmatrix);
    }

    public Path getTransformPath() {

        Matrix inverse = new Matrix();
        Path tPath = new Path();

        inverse.reset();
        if (matrix.equals(inverse)){
            tPath.set(this);
        }
        else {
            matrix.invert(inverse);
            transform(inverse, tPath);
        }
        return tPath;
    }
}

