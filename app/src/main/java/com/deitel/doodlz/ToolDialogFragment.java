package com.deitel.doodlz;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class ToolDialogFragment extends DialogFragment{

   public Dialog onCreateDialog(Bundle bundle) {
      // create dialog
      final AlertDialog.Builder builder =
              new AlertDialog.Builder(getActivity());
      View toolDialogView = getActivity().getLayoutInflater().inflate(
              R.layout.fragment_tool, null);

      builder.setView(toolDialogView); // add GUI to dialog

      // set the AlertDialog's message
      builder.setTitle(R.string.title_tool_dialog);

      final GridView gridview = (GridView) toolDialogView.findViewById(R.id.gridView);

      gridview.setAdapter(new GridViewAdapter(getActivity()));

      final DoodleView doodleView = getDoodleFragment().getDoodleView();
      gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> a, View view, int i, long l) {

            Log.d("ooo",String.valueOf(i));

            TextDialogFragment textDialog = new TextDialogFragment();


            if(i==14) textDialog.show(getFragmentManager(), "text dialog");
            doodleView.setSelectedTool(i);

            getDialog().dismiss();
         }

      });

      return builder.create(); // return dialog
   }

   private MainActivityFragment getDoodleFragment() {
      return (MainActivityFragment) getFragmentManager().findFragmentById(
              R.id.doodleFragment);
   }

   // tell MainActivityFragment that dialog is now displayed
   @Override
   public void onAttach(Activity activity) {
      super.onAttach(activity);
      MainActivityFragment fragment = getDoodleFragment();

      if (fragment != null)
         fragment.setDialogOnScreen(true);
   }

   // tell MainActivityFragment that dialog is no longer displayed
   @Override
   public void onDetach() {
      super.onDetach();
      MainActivityFragment fragment = getDoodleFragment();

      if (fragment != null)
         fragment.setDialogOnScreen(false);
   }

}
