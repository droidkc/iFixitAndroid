package com.dozuki.ifixit.ui.guide.create;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.guide.wizard.AbstractWizardModel;
import com.dozuki.ifixit.model.guide.wizard.ModelCallbacks;
import com.dozuki.ifixit.model.guide.wizard.Page;
import com.dozuki.ifixit.ui.IfixitActivity;
import com.dozuki.ifixit.ui.guide.create.wizard.PageFragmentCallbacks;
import com.dozuki.ifixit.ui.guide.create.wizard.ReviewFragment;
import com.dozuki.ifixit.ui.guide.create.wizard.StepPagerStrip;
import com.dozuki.ifixit.util.APIEvent;
import com.dozuki.ifixit.util.APIService;
import com.squareup.otto.Subscribe;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.Dialog;
import org.holoeverywhere.app.DialogFragment;
import org.holoeverywhere.widget.Button;

import java.util.List;

public class GuideIntroActivity extends IfixitActivity implements PageFragmentCallbacks,
 ReviewFragment.Callbacks,
 ModelCallbacks {
   private ViewPager mPager;
   private MyPagerAdapter mPagerAdapter;

   private boolean mEditingAfterReview;

   private AbstractWizardModel mWizardModel;

   private boolean mConsumePageSelectedEvent;

   private Button mNextButton;
   private Button mPrevButton;

   private List<Page> mCurrentPageSequence;
   private StepPagerStrip mStepPagerStrip;

   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.guide_create_intro_main);

      if (MainApplication.get().getSite().mGuideTypes != null) {
         initWizard();

         if (savedInstanceState != null) {
            mWizardModel.load(savedInstanceState.getBundle("model"));
         }
      } else {
         APIService.call(this, APIService.getSiteInfoAPICall());
      }

   }

   protected void initWizard() {
      mWizardModel = new GuideIntroWizardModel(this);
      mWizardModel.registerListener(this);
      mCurrentPageSequence = mWizardModel.getCurrentPageSequence();

      mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
      mPager = (ViewPager) findViewById(R.id.pager);
      mPager.setAdapter(mPagerAdapter);
      mStepPagerStrip = (StepPagerStrip) findViewById(R.id.strip);
      mStepPagerStrip.setOnPageSelectedListener(new StepPagerStrip.OnPageSelectedListener() {
         @Override
         public void onPageStripSelected(int position) {
            position = Math.min(mPagerAdapter.getCount() - 1, position);
            if (mPager.getCurrentItem() != position) {
               mPager.setCurrentItem(position);
            }
         }
      });

      mNextButton = (Button) findViewById(R.id.next_button);
      mPrevButton = (Button) findViewById(R.id.prev_button);

      mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
         @Override
         public void onPageSelected(int position) {
            mStepPagerStrip.setCurrentPage(position);

            if (mConsumePageSelectedEvent) {
               mConsumePageSelectedEvent = false;
               return;
            }

            mEditingAfterReview = false;
            updateBottomBar();
         }
      });

      mNextButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            if (mPager.getCurrentItem() == mCurrentPageSequence.size()) {
               DialogFragment dg = new DialogFragment() {
                  @Override
                  public Dialog onCreateDialog(Bundle savedInstanceState) {
                     return new AlertDialog.Builder(getActivity())
                      .setMessage(R.string.create_guide_intro_button)
                      .setPositiveButton(R.string.create_guide_intro_button, null)
                      .setNegativeButton(android.R.string.cancel, null)
                      .create();
                  }
               };
               dg.show(getSupportFragmentManager(), "place_order_dialog");
            } else {
               if (mEditingAfterReview) {
                  mPager.setCurrentItem(mPagerAdapter.getCount() - 1);
               } else {
                  mPager.setCurrentItem(mPager.getCurrentItem() + 1);
               }
            }
         }
      });

      mPrevButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
         }
      });

      onPageTreeChanged();
      updateBottomBar();

   }

   @Subscribe
   public void onSiteInfo(APIEvent.SiteInfo event) {
      if (!event.hasError()) {
         MainApplication.get().setSite(event.getResult());
         initWizard();
      } else {
         APIService.getErrorDialog(this, event.getError(),
          APIService.getSitesAPICall()).show();
      }
   }

   @Override
   public void onPageTreeChanged() {
      mCurrentPageSequence = mWizardModel.getCurrentPageSequence();
      recalculateCutOffPage();
      mStepPagerStrip.setPageCount(mCurrentPageSequence.size() + 1); // + 1 = review step
      mPagerAdapter.notifyDataSetChanged();
      updateBottomBar();
   }

   private void updateBottomBar() {
      int position = mPager.getCurrentItem();
      if (position == mCurrentPageSequence.size()) {
         mNextButton.setText(R.string.finish);
         mNextButton.setBackgroundResource(R.drawable.wizard_finish_background);
         //mNextButton.setTextAppearance(this, R.style.TextAppearanceFinish);
      } else {
         mNextButton.setText(mEditingAfterReview
          ? R.string.review
          : R.string.next);
         mNextButton.setBackgroundResource(R.drawable.wizard_selectable_item_background);
         TypedValue v = new TypedValue();
         getTheme().resolveAttribute(android.R.attr.textAppearanceMedium, v, true);
         mNextButton.setTextAppearance(this, v.resourceId);
         mNextButton.setEnabled(position != mPagerAdapter.getCutOffPage());
      }

      mPrevButton.setVisibility(position <= 0 ? View.INVISIBLE : View.VISIBLE);
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
      mWizardModel.unregisterListener(this);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putBundle("model", mWizardModel.save());
   }

   @Override
   public AbstractWizardModel onGetModel() {
      return mWizardModel;
   }

   @Override
   public void onEditScreenAfterReview(String key) {
      for (int i = mCurrentPageSequence.size() - 1; i >= 0; i--) {
         if (mCurrentPageSequence.get(i).getKey().equals(key)) {
            mConsumePageSelectedEvent = true;
            mEditingAfterReview = true;
            mPager.setCurrentItem(i);
            updateBottomBar();
            break;
         }
      }
   }

   @Override
   public void onPageDataChanged(Page page) {
      if (page.isRequired()) {
         if (recalculateCutOffPage()) {
            mPagerAdapter.notifyDataSetChanged();
            updateBottomBar();
         }
      }
   }

   @Override
   public Page onGetPage(String key) {
      return mWizardModel.findByKey(key);
   }

   private boolean recalculateCutOffPage() {
      // Cut off the pager adapter at first required page that isn't completed
      int cutOffPage = mCurrentPageSequence.size() + 1;
      for (int i = 0; i < mCurrentPageSequence.size(); i++) {
         Page page = mCurrentPageSequence.get(i);
         if (page.isRequired() && !page.isCompleted()) {
            cutOffPage = i;
            break;
         }
      }

      if (mPagerAdapter.getCutOffPage() != cutOffPage) {
         mPagerAdapter.setCutOffPage(cutOffPage);
         return true;
      }

      return false;
   }

   public class MyPagerAdapter extends FragmentStatePagerAdapter {
      private int mCutOffPage;
      private Fragment mPrimaryItem;

      public MyPagerAdapter(FragmentManager fm) {
         super(fm);
      }

      @Override
      public Fragment getItem(int i) {
         if (i >= mCurrentPageSequence.size()) {
            return new ReviewFragment();
         }

         return mCurrentPageSequence.get(i).createFragment();
      }

      @Override
      public int getItemPosition(Object object) {
         // TODO: be smarter about this
         if (object == mPrimaryItem) {
            // Re-use the current fragment (its position never changes)
            return POSITION_UNCHANGED;
         }

         return POSITION_NONE;
      }

      @Override
      public void setPrimaryItem(ViewGroup container, int position, Object object) {
         super.setPrimaryItem(container, position, object);
         mPrimaryItem = (Fragment) object;
      }

      @Override
      public int getCount() {
         return Math.min(mCutOffPage + 1, mCurrentPageSequence.size() + 1);
      }

      public void setCutOffPage(int cutOffPage) {
         if (cutOffPage < 0) {
            cutOffPage = Integer.MAX_VALUE;
         }
         mCutOffPage = cutOffPage;
      }

      public int getCutOffPage() {
         return mCutOffPage;
      }
   }

}
