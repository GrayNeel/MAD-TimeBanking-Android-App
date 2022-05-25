package it.polito.mad.g01_timebanking.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.polito.mad.g01_timebanking.R
import it.polito.mad.g01_timebanking.ui.MessageCollectionDiffCallback
import it.polito.mad.g01_timebanking.ui.chat.ChatViewModel
import it.polito.mad.g01_timebanking.ui.review.Review
import it.polito.mad.g01_timebanking.ui.review.ReviewViewModel
import java.util.*

data class MessageCollection (
    var chatId : String = "",
    var advId : String = "",
    var advTitle : String = "",
    var advOwnerUid : String = "",
    var advOwnerName : String = "",
    var requesterUid : String = "",
    var requesterName : String = "",
    var duration : String = "",
    var calendar : Date = Date(),
    var buyerHasRequested: Boolean = false,
    var ownerHasDecided : Boolean = false,
    var ownerHasReviewed : Boolean = false,
    var requesterHasReviewed : Boolean = false,
    var accepted : Boolean = false,
    val messages : MutableList<MessageDetails> = mutableListOf()
)

class MessageCollectionAdapter(
    private var data:List<MessageCollection>,
    private val reviewViewModel: ReviewViewModel,
    private val chatViewModel: ChatViewModel,
    private val navController: NavController
) : RecyclerView.Adapter<MessageCollectionAdapter.MessageCollectionViewHolder>() {

    class MessageCollectionViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private val chatCardView = v.findViewById<CardView>(R.id.chatCardView)
        private val chatAdvertisementTitle = v.findViewById<TextView>(R.id.chatAdvertisementTitle)
        private val fromTextView = v.findViewById<TextView>(R.id.fromTextView)
        private val lastMessageTextView = v.findViewById<TextView>(R.id.lastMessageTextView)
        private val reviewButton = v.findViewById<Button>(R.id.reviewButton)

        @SuppressLint("SetTextI18n")
        fun bind (messageCollection: MessageCollection, navController: NavController, chatViewModel: ChatViewModel, reviewViewModel : ReviewViewModel) {
            chatAdvertisementTitle.text = messageCollection.advTitle
            val isTheOwner = messageCollection.advOwnerUid == Firebase.auth.currentUser!!.uid
            if(isTheOwner) {
                fromTextView.text = "Requested from: ${messageCollection.requesterName}"
            } else {
                fromTextView.text = "Seller: ${messageCollection.advOwnerName}"
            }

            val messages = messageCollection.messages

            if(messages.size != 0)
                lastMessageTextView.text = messages[messages.lastIndex].content

            val calendar = Calendar.getInstance()
            calendar.time = messageCollection.calendar

            if(messageCollection.duration.isEmpty())
                return

            val duration = messageCollection.duration.split(":")

            calendar.add(Calendar.HOUR,duration[0].toInt())
            calendar.add(Calendar.MINUTE,duration[1].toInt())

            if(calendar.time < Calendar.getInstance().time && messageCollection.accepted) {

                if((isTheOwner && !messageCollection.ownerHasReviewed)) {
                    reviewButton.visibility = View.VISIBLE
                    reviewButton.setOnClickListener {
                        val newReview = Review().apply {
                            this.chatId = messageCollection.chatId
                            this.advId = messageCollection.advId
                            this.fromUid = Firebase.auth.currentUser!!.uid
                            this.toUid = messageCollection.requesterUid
                            this.reviewerIsTheOwner = true
                            this.reviewId =
                                "${Firebase.auth.currentUser!!.uid}-${messageCollection.requesterUid}-${messageCollection.advId}"
                        }

                        reviewViewModel.setReview(newReview)
                        navController.navigate(R.id.action_nav_my_chats_to_reviewFragment)
                    }
                }

                if((!isTheOwner && !messageCollection.requesterHasReviewed)) {
                    reviewButton.visibility = View.VISIBLE
                    reviewButton.setOnClickListener {
                        val newReview = Review().apply {
                            this.chatId = messageCollection.chatId
                            this.advId = messageCollection.advId
                            this.fromUid = Firebase.auth.currentUser!!.uid
                            this.toUid = messageCollection.advOwnerUid
                            this.reviewerIsTheOwner = false
                            this.reviewId = "${Firebase.auth.currentUser!!.uid}-${messageCollection.advOwnerUid}-${messageCollection.advId}"
                        }

                        reviewViewModel.setReview(newReview)
                        navController.navigate(R.id.action_nav_my_chats_to_reviewFragment)

                    }
                }
            } else {
                reviewButton.visibility = View.GONE
                chatCardView.setOnClickListener {
                    chatViewModel.setChatId(messageCollection.chatId)
                    navController.navigate(R.id.action_nav_my_chats_to_nav_chat_list)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageCollectionViewHolder {
        val v : View =
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.single_chat_layout, parent,false)

        return MessageCollectionViewHolder(v)
    }

    override fun onBindViewHolder(holder: MessageCollectionViewHolder, position: Int) {
        val messageCollection = data[position]

        holder.bind(messageCollection, navController, chatViewModel, reviewViewModel)
    }

    override fun getItemCount() = data.size

    fun setMyChats(newMessageCollection: List<MessageCollection>) {
        val diffs = DiffUtil.calculateDiff( MessageCollectionDiffCallback(data, newMessageCollection) )
        data = newMessageCollection.toList() //update data
        diffs.dispatchUpdatesTo(this) //animate UI
    }
}