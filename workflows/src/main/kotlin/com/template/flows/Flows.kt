package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.flows.FinalityFlow

import net.corda.core.flows.CollectSignaturesFlow

import net.corda.core.transactions.SignedTransaction

import java.util.stream.Collectors

import net.corda.core.flows.FlowSession

import net.corda.core.identity.Party

import com.template.contracts.TemplateContract
import com.template.states.IOUState

import net.corda.core.transactions.TransactionBuilder

import com.template.states.TemplateState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import org.hibernate.Transaction


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class IOUFlow(
    val iouValue: Int,
    val otherParty: Party
) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()
    @Suspendable
    override fun call() {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        // 1.We create the transaction components.
        val outputState = IOUState(iouValue, ourIdentity, otherParty)
        val command = Command(TemplateContract.Commands.Create(), ourIdentity.owningKey)

        // 2.We create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary = notary).addOutputState(outputState, TemplateContract.ID).addCommand(command)

        // 3.We sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // Creating a session with the other party.
        val otherPartySession = initiateFlow(otherParty)

        // 4.We finalise the transaction and then send it to the counterparty.
        subFlow(FinalityFlow(signedTx, otherPartySession))

    }
}



@InitiatedBy(IOUFlow::class)
class IOUFlowResponder(private val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(otherPartySession))
    }
}
