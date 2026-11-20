package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ListingType
import com.example.data.model.PropertyCategory
import com.example.data.model.PropertyFormatters
import com.example.data.model.PropertyListing
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

data class AiAssistantMessage(
  val id: String = UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val isStreaming: Boolean = false,
)

data class PropertyAiUiState(
  val messages: List<AiAssistantMessage> = emptyList(),
  val isGenerating: Boolean = false,
  val errorMessage: String? = null,
)

class PropertyAiViewModel : ViewModel() {

  private val model by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      systemInstruction =
        content {
          text(
            "You are the AI Real Estate Assistant built into the Property Broker app in India. " +
              "Help buyers, sellers, renters, and property brokers with practical, concise, accurate guidance: " +
              "matching properties from the provided catalog, estimating Indian EMI and stamp duty, " +
              "drafting professional Sale/Rent/Heavy Deposit listing descriptions, comparing Apartment/Villa/Shop/Gala options, " +
              "and offering clear negotiation and verification checklists. " +
              "Format responses cleanly with short bullet points and INR values (₹, Lakhs, Crores). " +
              "Never invent fake owner phone numbers."
          )
        },
    )
  }

  private val _uiState = MutableStateFlow(PropertyAiUiState())
  val uiState: StateFlow<PropertyAiUiState> = _uiState.asStateFlow()

  private var lastPrompt: String? = null
  private var lastCatalogSnapshot: List<PropertyListing> = emptyList()

  fun askAssistant(
    userPrompt: String,
    catalog: List<PropertyListing>,
  ) {
    val cleanPrompt = userPrompt.trim()
    if (cleanPrompt.isEmpty() || _uiState.value.isGenerating) return

    lastPrompt = cleanPrompt
    lastCatalogSnapshot = catalog

    val userMessage = AiAssistantMessage(isUser = true, text = cleanPrompt)
    val assistantId = UUID.randomUUID().toString()
    val placeholderReply =
      AiAssistantMessage(id = assistantId, isUser = false, text = "", isStreaming = true)

    _uiState.update { state ->
      state.copy(
        messages = state.messages + userMessage + placeholderReply,
        isGenerating = true,
        errorMessage = null,
      )
    }

    val fullPrompt = buildContextualPrompt(cleanPrompt, catalog, _uiState.value.messages.dropLast(2))

    viewModelScope.launch {
      val accumulated = StringBuilder()
      try {
        withTimeout(INFERENCE_TIMEOUT_MS) {
          model.generateContentStream(fullPrompt).collect { chunk ->
            val piece = chunk.text.orEmpty()
            if (piece.isNotEmpty()) {
              accumulated.append(piece)
              val currentText = accumulated.toString()
              _uiState.update { state ->
                state.copy(
                  messages =
                    state.messages.map { msg ->
                      if (msg.id == assistantId) msg.copy(text = currentText, isStreaming = true)
                      else msg
                    }
                )
              }
            }
          }
        }

        val finalReply = accumulated.toString().trim()
        if (finalReply.isNotEmpty()) {
          _uiState.update { state ->
            state.copy(
              isGenerating = false,
              messages =
                state.messages.map { msg ->
                  if (msg.id == assistantId) msg.copy(text = finalReply, isStreaming = false)
                  else msg
                },
            )
          }
        } else {
          removeStreamingPlaceholder(
            assistantId = assistantId,
            error = "AI returned an empty response. Please try again.",
          )
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI generation timed out", e)
        removeStreamingPlaceholder(
          assistantId = assistantId,
          error = "AI request timed out after 60 seconds. Check your connection and try again.",
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI generation failed", e)
        removeStreamingPlaceholder(
          assistantId = assistantId,
          error = e.localizedMessage ?: "Unable to reach AI assistant right now.",
        )
      }
    }
  }

  fun retryLastPrompt() {
    val prompt = lastPrompt ?: return
    _uiState.update { it.copy(errorMessage = null) }
    askAssistant(prompt, lastCatalogSnapshot)
  }

  fun clearConversation() {
    if (_uiState.value.isGenerating) return
    _uiState.value = PropertyAiUiState()
    lastPrompt = null
  }

  private fun removeStreamingPlaceholder(assistantId: String, error: String) {
    _uiState.update { state ->
      state.copy(
        isGenerating = false,
        errorMessage = error,
        messages = state.messages.filterNot { it.id == assistantId && it.text.isBlank() },
      )
    }
  }

  private fun buildContextualPrompt(
    userPrompt: String,
    catalog: List<PropertyListing>,
    priorMessages: List<AiAssistantMessage>,
  ): String {
    val catalogSummary =
      if (catalog.isEmpty()) {
        "Current app listings: None posted yet."
      } else {
        catalog
          .take(15)
          .joinToString(separator = "\n") { item ->
            val type = ListingType.fromRaw(item.listingType).label
            val cat = PropertyCategory.fromRaw(item.propertyCategory).label
            val price = PropertyFormatters.formatInrPrice(item.price, item.listingType)
            val area = if (item.areaSqFt > 0L) "${item.areaSqFt} sq.ft" else "Area N/A"
            val bhk = if (item.bedrooms > 0L) ", ${item.bedrooms} BHK" else ""
            "- ${item.title} | $type | $cat ($area$bhk) | ${item.location} | $price"
          }
      }

    val recentHistory =
      priorMessages
        .takeLast(4)
        .filter { it.text.isNotBlank() }
        .joinToString(separator = "\n") { msg ->
          val speaker = if (msg.isUser) "User" else "Assistant"
          "$speaker: ${msg.text.take(400)}"
        }

    return buildString {
      appendLine("Available Properties in App:")
      appendLine(catalogSummary)
      if (recentHistory.isNotBlank()) {
        appendLine()
        appendLine("Recent Conversation:")
        appendLine(recentHistory)
      }
      appendLine()
      appendLine("User Request:")
      append(userPrompt)
    }
  }

  private companion object {
    const val TAG = "PropertyAiVM"
    const val MODEL_NAME = "gemini-3.8-flash"
    const val INFERENCE_TIMEOUT_MS = 60_000L
  }
}
