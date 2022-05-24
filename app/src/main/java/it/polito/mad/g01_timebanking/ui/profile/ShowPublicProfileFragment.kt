package it.polito.mad.g01_timebanking.ui.profile

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import it.polito.mad.g01_timebanking.R
import it.polito.mad.g01_timebanking.UserInfo
import it.polito.mad.g01_timebanking.UserKey
import it.polito.mad.g01_timebanking.helpers.FileHelper

class ShowPublicProfileFragment : Fragment() {
    private val profileViewModel : ProfileViewModel by activityViewModels()

    companion object {
        private const val TAG = "ShowProfileActivity"
    }
    private lateinit var scrollView: ScrollView
    private lateinit var frameView: FrameLayout
    private lateinit var tvFullName: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvBiography: TextView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var skillGroup: ChipGroup
    private lateinit var noSkills: TextView

    private lateinit var actUserInfo : UserInfo

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_show_profile, container, false)

        setHasOptionsMenu(true)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch views
        scrollView = view.findViewById(R.id.sv)
        frameView = view.findViewById(R.id.frameView1)
        tvFullName = view.findViewById(R.id.fullname)
        tvNickname = view.findViewById(R.id.nickname)
        tvEmail = view.findViewById(R.id.email)
        tvLocation = view.findViewById(R.id.location)
        tvBiography = view.findViewById(R.id.biography)
        ivProfilePicture = view.findViewById(R.id.profilePicture)
        skillGroup = view.findViewById(R.id.skillgroup)
        noSkills = view.findViewById(R.id.noSkillsTextView)

        profileViewModel.pubUserTmpPath.observe(this.viewLifecycleOwner){
            if (it != UserKey.PROFILE_PICTURE_PATH_PLACEHOLDER)
                FileHelper.readImage(it, ivProfilePicture)
            else {
                ivProfilePicture.setImageResource(R.drawable.avatar)
            }
        }

        profileViewModel.pubUser.observe(this.viewLifecycleOwner) {
            actUserInfo = it
            tvFullName.text = it.fullName
            tvNickname.text = it.nickname
            tvEmail.text = it.email
            tvLocation.text = it.location
            tvBiography.text = it.biography

            skillGroup.removeAllViews()

            if(it.skills.isEmpty())
                noSkills.isVisible = true
            else
                it.skills
                    .forEach{ content ->
                        val chip = Chip(context)
                        chip.text = content
                        chip.isCheckable = false
                        chip.isClickable = true
                        skillGroup.addView(chip)
                    }.also{ noSkills.isVisible = false }
        }


//        profileViewModel.profilePicturePath.observe(this.viewLifecycleOwner) {
//            if (it != UserKey.PROFILE_PICTURE_PATH_PLACEHOLDER) {
//                FileHelper.readImage(it, ivProfilePicture)
//            }
//        }

        //the only way to set height image to 1/3 of the screen is programmatically
        //This is ue to the fact that we use a scroll view with a bio with variable length
        arrangeViewByRatio(view)

        if(!FileHelper.isExternalStorageWritable())
            Log.e(TAG, "No external volume mounted")
    }

//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        inflater.inflate(R.menu.user_menu, menu)
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle item selection
//        return NavigationUI.onNavDestinationSelected(
//            item,
//            requireView().findNavController())
//                || super.onOptionsItemSelected(item)
//    }

    override fun onPause() {
        // This updates ViewModel if the show is disappearing because the edit is being opened
        profileViewModel.setPublicUserInfo(actUserInfo)
        super.onPause()
    }

    private fun arrangeViewByRatio(view: View) {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            scrollView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    frameView.post {
                        frameView.layoutParams =
                            LinearLayout.LayoutParams(scrollView.width, scrollView.height / 3)
                    }
                    scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    //Resize consequently cardView with image
                    val cardView = view.findViewById<CardView>(R.id.imageCard)
                    val relativeDimension = scrollView.height / 3 - 32
                    //I want a square box for the image that doesn't fit all the space in the parent frameView
                    cardView.layoutParams.width = relativeDimension
                    cardView.layoutParams.height = relativeDimension
                    //different from before because cardView doesn't work with LinearLayout.LayoutP....

                    cardView.radius = (relativeDimension/2).toFloat()
                }
            })
        }
    }
}