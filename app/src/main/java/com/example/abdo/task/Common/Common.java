package com.example.abdo.task.Common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.example.abdo.task.Remote.IGeoCoordinates;
import com.example.abdo.task.Remote.RetrofitClient;

public class Common {

    public static final String baseUrl="https://maps.googleapis.com/";


    public static final int PICK_IMAGE_REQUEST=71;


    public static IGeoCoordinates getGeoCodeService ()
    {
        return RetrofitClient.getClient(baseUrl).create(IGeoCoordinates.class);
    }

    public static Bitmap scaleBitmap(Bitmap bitmap , int newWidth , int newHeight)
    {
        Bitmap scaledBitmap =Bitmap.createBitmap(newWidth,newHeight,Bitmap.Config.ARGB_8888);
        float scaleX=newWidth/(float)bitmap.getWidth();
        float scaleY=newHeight/(float)bitmap.getHeight();
        float pivotX=0,pivotY=0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX,scaleY,pivotX,pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap,0,0 ,new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;

    }



}