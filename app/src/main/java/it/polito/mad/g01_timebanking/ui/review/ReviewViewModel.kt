package it.polito.mad.g01_timebanking.ui.review

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import it.polito.mad.g01_timebanking.adapters.MessageCollection

class ReviewViewModel(val a: Application) : AndroidViewModel(a) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth

    private val pvtReview = MutableLiveData<Review>().also {
        it.value = Review()
    }

    val review : LiveData<Review> = pvtReview

    fun sendReview(review: Review, reviewText: String, rating: Int) {
        review.text = reviewText
        review.rating = rating
        db.collection("reviews")
            .document(review.reviewId)
            .set(review)
            .addOnSuccessListener {
                Log.d("Send_Review","Success!")
                db.collection("chats")
                    .document(review.chatId)
                    .get()
                    .addOnSuccessListener {
                        val chat = it.toObject(MessageCollection::class.java) ?: MessageCollection()
                        if(review.toUid == chat.advOwnerUid)
                            chat.requesterHasReviewed = true
                        else
                            chat.ownerHasReviewed = true

                        db.collection("chats").document(review.chatId).set(chat).addOnSuccessListener {
                            Log.d("Has_Reviewed", "Success!")
                        }
                    }
            }
    }

    fun setReview(newReview: Review) {
        pvtReview.value = newReview
    }

}

private fun DocumentSnapshot.toReview(): Review {
    return this.toObject(Review::class.java) ?: Review()
}