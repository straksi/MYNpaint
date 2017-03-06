package com.deitel.doodlz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class GridViewAdapter extends BaseAdapter {
   private Context mContext;

   public GridViewAdapter(Context c) {
      mContext = c;
   }

   public int getCount() {
      return itemImages.length;
   }

   public Object getItem(int position) {
      return itemImages[position];
   }

   public long getItemId(int position) {
      return position;
   }

   @Override
   public View getView(int position, View convertView, ViewGroup parent) {
      // TODO Auto-generated method stub


      LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
      View grid = inflater.inflate(R.layout.grid_item, parent, false);


      ImageView imageView = (ImageView) grid.findViewById(R.id.img_v);
      TextView textView = (TextView) grid.findViewById(R.id.txt_v);
      imageView.setImageResource(itemImages[position]);
      textView.setText(itemNames[position]);

      return grid;
   }

   // references to our images
   public Integer[] itemImages = {R.drawable.pencil,R.drawable.line1, R.drawable.multi2, R.drawable.eraser11,
           R.drawable.circle1, R.drawable.circle22,
           R.drawable.oval1, R.drawable.oval22,
           R.drawable.ic_square_24dp, R.drawable.square22,
           R.drawable.rectangle1, R.drawable.ic_rectangle_fill_24dp,
           R.drawable.text1,R.drawable.hand2};
   public String[] itemNames = {"Pencil","Line","Polygonal line","Eraser","Circle","Filled Circle",
           "Oval","Filled Oval","Square","Filled Square", "Rectangle","Filled Rectangle","Text","Hand mode"};
}
