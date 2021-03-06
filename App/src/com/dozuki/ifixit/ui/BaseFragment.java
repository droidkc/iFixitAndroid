package com.dozuki.ifixit.ui;

import com.actionbarsherlock.app.SherlockFragment;
import com.dozuki.ifixit.App;

/**
 * Base class for Fragments. Handles bus registration and unregistration
 * in onResume/onPause.
 *
 * Note: This is basically a duplicate of other Base*Fragment classes.
 */
public class BaseFragment extends SherlockFragment {

   @Override
   public void onResume() {
      App.getBus().register(this);
      super.onResume();
   }

   @Override
   public void onPause() {
      App.getBus().unregister(this);
      super.onPause();
   }
}
