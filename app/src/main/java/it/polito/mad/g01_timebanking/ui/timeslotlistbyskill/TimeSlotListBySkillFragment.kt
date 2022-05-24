package it.polito.mad.g01_timebanking.ui.timeslotlistbyskill

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.g01_timebanking.R
import it.polito.mad.g01_timebanking.databinding.FragmentTimeSlotListBinding
import it.polito.mad.g01_timebanking.adapters.AdvertisementAdapter
import it.polito.mad.g01_timebanking.ui.timeslotdetails.TimeSlotDetailsViewModel


class TimeSlotListBySkillFragment : Fragment() {
    private val timeSlotDetailsViewModel : TimeSlotDetailsViewModel by activityViewModels()
    private val timeSlotListBySkillViewModel : TimeSlotListBySkillViewModel by activityViewModels()

    private var _binding: FragmentTimeSlotListBinding? = null

    private lateinit var adapter : AdvertisementAdapter

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimeSlotListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sortingButton = view.findViewById<Button>(R.id.sortingButton)
        val filteringButton = view.findViewById<Button>(R.id.filterButton)

        sortingButton.setOnClickListener{
            showPopup(sortingButton)
        }

        filteringButton.setOnClickListener {
            view.findNavController().navigate(R.id.action_nav_adv_list_by_skill_to_timeSlotFiltersFragment)
        }

        sortingButton.visibility = View.VISIBLE
        filteringButton.visibility = View.VISIBLE

        val recyclerViewAdv = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerViewAdv.layoutManager = LinearLayoutManager(context)

        val emptyAdvText = view.findViewById<TextView>(R.id.emptyAdvertisementsText)


        adapter = AdvertisementAdapter(listOf(), timeSlotDetailsViewModel, true)
        recyclerViewAdv.adapter = adapter

        timeSlotListBySkillViewModel.advList.observe(this.viewLifecycleOwner){
            if (it.isEmpty()) {
                recyclerViewAdv.visibility = View.GONE
                emptyAdvText.visibility = View.VISIBLE
            } else {
                recyclerViewAdv.visibility = View.VISIBLE
                emptyAdvText.visibility = View.GONE
            }
            adapter.setAdvertisements(it)
        }

    }

    private fun showPopup(v: View) {
        val popup = PopupMenu(this.requireContext(), v)
        val inflater = popup.menuInflater
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.filterFromAtoZ -> timeSlotListBySkillViewModel.sortAtoZ()
                R.id.filterFromZtoA -> timeSlotListBySkillViewModel.sortZtoA()
                R.id.filterMostRecents -> timeSlotListBySkillViewModel.sortMostRecents()
                R.id.filterLessRecents -> timeSlotListBySkillViewModel.sortLessRecents()
            }
            true
        }


        inflater.inflate(R.menu.sorting_menu, popup.menu)
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}