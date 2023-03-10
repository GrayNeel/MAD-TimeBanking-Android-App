package it.polito.mad.g01_timebanking.ui.chat

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import it.polito.mad.g01_timebanking.UserInfo
import it.polito.mad.g01_timebanking.adapters.AdvertisementDetails
import it.polito.mad.g01_timebanking.adapters.MessageCollection
import it.polito.mad.g01_timebanking.adapters.MessageDetails
import it.polito.mad.g01_timebanking.ui.timeslotdetails.TimeSlotDetailsViewModel
import java.text.DecimalFormat
import java.util.*

class ChatViewModel(val a: Application) : AndroidViewModel(a) {
    private var _messagesCollection : MessageCollection = MessageCollection()


    private val pvtMessagesCollection = MutableLiveData<MessageCollection>().also {
        it.value = _messagesCollection
    }

    val messagesCollection : LiveData<MessageCollection> = pvtMessagesCollection

    private val pvtReceiverUid = MutableLiveData<String>().also {
        it.value = ""
    }

    val receiverUid : LiveData<String> = pvtReceiverUid

    private val pvtAdvertisement = MutableLiveData<AdvertisementDetails>().also {
        it.value = AdvertisementDetails()
    }

    val advertisement : LiveData<AdvertisementDetails> = pvtAdvertisement

    private val pvtMessageText = MutableLiveData<String>().also {
        it.value = ""
    }

    val messageText : LiveData<String> = pvtMessageText

    private val pvtChatId = MutableLiveData<String>().also {
        it.value = ""
    }

    val chatId : LiveData<String> = pvtChatId

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private var messagesListener: ListenerRegistration? = null

    fun getMessagesList(chatId: String) {
        messagesListener = db.collection("chats")
            .document(chatId)
            .addSnapshotListener { value, e ->
                if (e == null && value?.exists() == true) {
                    Log.d("Messages_Listener", "Data found on database. Updating!")
                    _messagesCollection = value.toMessageCollection()
                    _messagesCollection.messages = _messagesCollection.messages.map {
                        if(!it.readBy.contains(auth.currentUser!!.uid))
                            it.readBy.add(auth.currentUser!!.uid)
                        it }.toMutableList()
                    pvtMessagesCollection.value = _messagesCollection
                } else if (e == null) {
                    Log.d("Messages_Listener", "Data not found on database.")

                    db.collection("users")
                        .document(auth.currentUser!!.uid)
                        .get()
                        .addOnSuccessListener{ user ->
                            val userInfo = user.toObject(UserInfo::class.java) ?: UserInfo()

                            db.collection("users")
                                .document(pvtAdvertisement.value!!.uid)
                                .get()
                                .addOnSuccessListener { advOwner ->
                                    val userInfoOwner = advOwner.toObject(UserInfo::class.java) ?: UserInfo()

                                    val newCollection = MessageCollection().apply {
                                        this.advId = advertisement.value!!.id
                                        this.advTitle = pvtAdvertisement.value!!.title
                                        this.calendar = pvtAdvertisement.value!!.calendar
                                        this.duration = pvtAdvertisement.value!!.duration
                                        this.requesterName = userInfo.fullName
                                        this.chatId = chatId
                                        this.requesterUid = auth.currentUser!!.uid
                                        this.advOwnerUid = advertisement.value!!.uid
                                        this.advOwnerName = userInfoOwner.fullName
                                        this.ownerHasDecided = false
                                        this.accepted = false
                                    }
                                    addOrUpdateData(newCollection, chatId)
                                }
                            }
                }
            }
    }

    private fun addOrUpdateData(newCollection: MessageCollection, chatId: String) {
        db.collection("chats").document(chatId).set(newCollection)
            .addOnSuccessListener {
                Log.d("InsertOrUpdateMesColl", "Success: $it")
                _messagesCollection = newCollection
                pvtMessagesCollection.value = _messagesCollection
            }
            .addOnFailureListener {
                Log.d("InsertOrUpdateMesColl", "Exception: ${it.message}")
                Toast.makeText(
                    a.applicationContext,
                    "Failed updating data. Try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun takeDecision(collection: MessageCollection, accepted: Boolean) {
        // Get advertisements info
        db.collection("advertisements")
            .document(collection.advId)
            .get()
            .addOnSuccessListener { adv ->
                val advInfo =
                    adv.toObject(AdvertisementDetails::class.java) ?: AdvertisementDetails()

                // Get requester info
                db.collection("users")
                    .document(collection.requesterUid)
                    .get()
                    .addOnSuccessListener { requester ->
                        val requesterInfo = requester.toObject(UserInfo::class.java) ?: UserInfo()

                        // Get seller info
                        db.collection("users")
                            .document(collection.advOwnerUid)
                            .get()
                            .addOnSuccessListener { seller ->
                                val sellerInfo = seller.toObject(UserInfo::class.java) ?: UserInfo()

                                // Check if it is possible to sell advertisement
                                if (requesterInfo.balance.toAmountTime() >= collection.duration.toAmountTime()) {
                                    advInfo.sold = true
                                    decreaseUsageOfSkills(advInfo.skills.toSet())
                                    advInfo.soldToUid = collection.requesterUid
                                    val newRequesterBalance =
                                        requesterInfo.balance.toAmountTime() - collection.duration.toAmountTime()
                                    requesterInfo.balance = newRequesterBalance.toDurationString()

                                    val newSellerBalance =
                                        sellerInfo.balance.toAmountTime() + collection.duration.toAmountTime()
                                    sellerInfo.balance = newSellerBalance.toDurationString()

                                    // Update balance
                                    db.collection("users").document(collection.requesterUid)
                                        .set(requesterInfo)
                                    db.collection("users").document(collection.advOwnerUid)
                                        .set(sellerInfo)

                                    // Update advertisement status
                                    db.collection("advertisements").document(collection.advId)
                                        .set(advInfo)
                                        .addOnSuccessListener {
                                            Log.d(
                                                "InsertOrUpdateMesColl",
                                                "Success: $it"
                                            )
                                        }

                                    // Update all chats as no more available
                                    db.collection("chats")
                                        .whereEqualTo("advId",advInfo.id)
                                        .whereNotEqualTo("chatId",collection.chatId)
                                        .get()
                                        .addOnSuccessListener {
                                            for(doc in it) {
                                                val chat = doc.toObject(MessageCollection::class.java)
                                                chat.accepted = false
                                                chat.buyerHasRequested = true
                                                chat.ownerHasDecided = true
                                                db.collection("chats").document(doc.id).set(chat)
                                            }
                                        }

                                    // Update collection
                                    collection.ownerHasDecided = true
                                    collection.accepted = accepted
                                    db.collection("chats").document(collection.chatId).set(collection)
                                        .addOnSuccessListener {
                                            _messagesCollection = collection
                                            pvtMessagesCollection.value = _messagesCollection
                                        }

                                } else {
                                    Toast.makeText(
                                        a.applicationContext,
                                        "The buyer has not enough money. Can't accept",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
            }
            .addOnFailureListener {
                Log.d("InsertOrUpdateMesColl", "Exception: ${it.message}")
                Toast.makeText(
                    a.applicationContext,
                    "Failed updating data. Try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun decreaseUsageOfSkills(removedSkills: Set<String>) {
        removedSkills.forEach { removedSkill ->
            db.collection("suggestedSkills").document(removedSkill)
                .update(
                    "usage_in_adv",
                    FieldValue.increment(-1L)
                ).addOnSuccessListener {
                    Log.d("AdvSkill", "skill $removedSkill decremented")
                }
        }
    }

    fun increaseUsageOfSkills(addedSkills: Set<String>) {
        addedSkills.forEach { addedSkill ->
            db.collection("suggestedSkills").document(addedSkill)
                .set(
                    hashMapOf(
                        "name" to addedSkill,
                        "usage_in_adv" to FieldValue.increment(1L),
                        "usage_in_user" to FieldValue.increment(0L)
                    ),
                    SetOptions.merge()
                )
        }
    }

    private fun DocumentSnapshot.toMessageCollection(): MessageCollection {
        return this.toObject(MessageCollection::class.java) ?: MessageCollection()
    }

    override fun onCleared() {
        messagesListener?.remove()
    }

    fun sendMessage(chatId: String) {
        val message = MessageDetails(
            "",
            receiverUid.value!!,
            auth.currentUser!!.uid,
            Calendar.getInstance().time,
            messageText.value!!,
            readBy = mutableListOf(auth.currentUser!!.uid)
            )

        _messagesCollection.messages.add(message)
        addOrUpdateData(_messagesCollection, chatId)
        pvtMessageText.value = ""
    }

    fun markAsRead(){
        addOrUpdateData(_messagesCollection, chatId.value!!)
    }

    fun setMessageText(text: String) {
        pvtMessageText.value = text
    }

    fun setChatId(chatId: String) {
        pvtChatId.value = chatId
    }

    fun buyerTakesDecision(chat: MessageCollection, requested: Boolean) {
        chat.buyerHasRequested = requested

        db.collection("chats").document(chat.chatId).set(chat)
            .addOnSuccessListener {
                Log.d("InsertOrUpdateMesColl", "Success!")
                _messagesCollection = chat
                pvtMessagesCollection.value = _messagesCollection
            }
            .addOnFailureListener {
                Log.d("InsertOrUpdateMesColl", "Exception: ${it.message}")
                Toast.makeText(
                    a.applicationContext,
                    "Failed updating data. Try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun setChat(adv: AdvertisementDetails) {
        pvtReceiverUid.value = adv.uid
        pvtAdvertisement.value = adv
        pvtChatId.value = "${Firebase.auth.currentUser!!.uid}-${adv.uid}-${adv.id}"
    }
}

fun String.toAmountTime() : Int {
    val split = this.split(":")
    var result = 0

    if(split.size != 2)
        return result

    try {
        val hours = split[0].toInt()
        val minutes = split[1].toInt()

        result = (hours*60)+minutes
    } catch(e: Exception) {
        return result
    }
    return result
}

fun Int.toDurationString() : String {
    val hours = this/60
    val minutes = this%60

    val f = DecimalFormat("00")

    return "${f.format(hours)}:${f.format(minutes)}"
}