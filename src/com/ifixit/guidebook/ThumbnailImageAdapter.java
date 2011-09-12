package com.ifixit.guidebook;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

public class ThumbnailImageAdapter extends BaseAdapter {
   private Context mContext;
   private GuideStep mStep;

   private ImageManager mImageManager;

   public ThumbnailImageAdapter(Context context, GuideStep step,
    ImageManager imageManager) {
      mContext = context;
      mStep = step;
       
      TypedArray attr = mContext.obtainStyledAttributes(R.styleable.thumbnail_gallery);

      attr.recycle();
      
      mImageManager = imageManager;
   }

   public int getCount() {
       return mStep.getImages().size();
   }

   public Object getItem(int position) {
       return position;
   }

   public long getItemId(int position) {
       return position;
   }

   public View getView(int position, View convertView, ViewGroup parent) {
      ImageView imageView;
      if (convertView == null) 
         imageView = new ImageView(mContext);
      else 
         imageView = (ImageView) convertView;
      
      imageView.setLayoutParams(new Gallery.LayoutParams(
       GuideStepView.THUMBNAIL_WIDTH, GuideStepView.THUMBNAIL_HEIGHT));
      imageView.setScaleType(ImageView.ScaleType.FIT_XY);
      
      mImageManager.displayImage(mStep.mImages.get(position).mText + ".thumbnail",
       (Activity)mContext, imageView);             
        
      return (View)imageView;
   }
}
