package com.jvtnascimento.picpay.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import com.jvtnascimento.picpay.R
import com.jvtnascimento.picpay.application.BaseApplication
import com.jvtnascimento.picpay.dagger.GlideApp
import com.jvtnascimento.picpay.models.*
import com.jvtnascimento.picpay.presenter.CreditCardPresenter
import com.jvtnascimento.picpay.presenter.MainPresenter
import com.jvtnascimento.picpay.presenter.contracts.MainPresenterContractInterface
import com.jvtnascimento.picpay.view.components.Toaster
import com.jvtnascimento.picpay.view.contracts.CreditCardViewContractInterface
import com.jvtnascimento.picpay.view.contracts.MainViewContractInterface
import de.hdodenhof.circleimageview.CircleImageView
import javax.inject.Inject

class PaymentActivity : AppCompatActivity(), CreditCardViewContractInterface, MainViewContractInterface {

    @BindView(R.id.userPicture) lateinit var userPicture: CircleImageView
    @BindView(R.id.userUsername) lateinit var userUsername: TextView
    @BindView(R.id.value) lateinit var value: EditText
    @BindView(R.id.prefix) lateinit var prefix: TextView
    @BindView(R.id.creditCardInfo) lateinit var creditCardInfo: TextView
    @BindView(R.id.editCreditCardButton) lateinit var editCreditCardButton: Button
    @BindView(R.id.bottomButton) lateinit var bottomButton: Button
    @BindView(R.id.progressBar) lateinit var progressBar: ProgressBar

    @Inject lateinit var creditCardPresenter: CreditCardPresenter
    @Inject lateinit var mainPresenter: MainPresenter

    private val CREDIT_CARD_RESULT: Int = 1
    private val EDIT_CREDIT_CARD_RESULT: Int = 2

    private var user: User? = null
    private var creditCard: CreditCard? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        ButterKnife.bind(this)

        if (this.intent.hasExtra("user"))
            this.user = this.intent.getSerializableExtra("user") as User

        this.configureComponents()
        this.configureView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CREDIT_CARD_RESULT) {
                if (data != null) {
                    val creditCard = data.getSerializableExtra("creditCard") as CreditCard
                    if (creditCard !== this.creditCard) {
                        this.creditCard = creditCard
                        this.creditCardInfo.text = "Cartão Master " + creditCard.firstFourNumbers
                    }
                }
            }
            else if (requestCode == EDIT_CREDIT_CARD_RESULT) {
                if (data != null) {
                    val creditCard = data.getSerializableExtra("creditCard") as CreditCard
                    if (creditCard !== this.creditCard) {
                        this.creditCard = creditCard
                        this.creditCardInfo.text = "Cartão Master " + creditCard.firstFourNumbers
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun getCreditCard(creditCard: CreditCard?) {
        this.creditCard = creditCard
    }

    override fun showProgressBar() {
        this.progressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        this.progressBar.visibility = View.GONE
    }

    override fun showError(error: Throwable) {
        Toaster.showError(error, this)
    }

    override fun showResult(transaction: TransactionResponse) {
        Toaster.showMessage("Pagamento efetuado com sucesso! =)", this)
    }

    private fun configureComponents() {
        (application as BaseApplication).component.inject(this)
        this.creditCardPresenter.attach(this)
        this.mainPresenter.attach(this)
        this.creditCardPresenter.findOne()

        (this as AppCompatActivity).supportActionBar!!.title = ""
        (this as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        (this as AppCompatActivity).supportActionBar!!.setDisplayShowHomeEnabled(true)
        (this as AppCompatActivity).supportActionBar!!.elevation = 0f

        this.editCreditCardButton.setOnClickListener {
            this.onEditCreditCardButtonTap()
        }

        this.bottomButton.setOnClickListener {
            if (this.value.text.toString() != "") {
                this.onBottomButtonTap()
            }
        }

        val self = this
        this.value.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (value.text.toString() != "") {
                    bottomButton.background = ContextCompat.getDrawable(self, R.drawable.shape_bottom_button)
                    prefix.setTextColor(Color.parseColor("#21C15F"))
                }
                else {
                    bottomButton.background = ContextCompat.getDrawable(self, R.drawable.shape_disabled_bottom_button)
                    prefix.setTextColor(Color.parseColor("#7B7D7F"))
                }
            }
        })
    }

    private fun configureView() {
        if (this.creditCard != null) {
            this.creditCardInfo.text = "Cartão Master " + this.creditCard!!.firstFourNumbers
        } else {
            showCreditCardPrimingActivity()
        }

        if (this.user != null) {

            GlideApp.with(this)
                .load(this.user!!.img)
                .placeholder(R.color.imageViewBackground)
                .into(userPicture)

            this.userUsername.setText(this.user!!.username)
        }
    }

    private fun showCreditCardPrimingActivity() {
        val intent = Intent(this, CreditCardPrimingActivity::class.java)
        startActivityForResult(intent, CREDIT_CARD_RESULT)
    }

    private fun onEditCreditCardButtonTap() {
        val intent = Intent(this, CreditCardRegisterActivity::class.java)
        intent.putExtra("creditCard", this.creditCard)
        startActivityForResult(intent, EDIT_CREDIT_CARD_RESULT)
    }

    private fun onBottomButtonTap() {
        if (this.creditCard != null && this.user != null) {
            val transaction = TransactionRequest(
                this.value.text.toString().toFloat(),
                this.creditCard!!.number,
                this.creditCard!!.expirationDate,
                this.creditCard!!.cvv,
                this.user!!.id
            )

            this.mainPresenter.pay(transaction)
        }
    }
}
