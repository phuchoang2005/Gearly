// ChatBox.jsx
import React, { useState, useRef, useEffect } from "react";
import { Bot, Send, X } from "lucide-react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { useNavigate } from "react-router-dom";
import ReactMarkdown from "react-markdown";

const ChatBox = () => {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    { sender: "ai", text: "Hi! I'm your AI assistant, ready to help!" },
  ]);
  const [input, setInput] = useState("");

  // Typing indicator state: true when waiting for AI response
  const [isTyping, setIsTyping] = useState(false);

  const chatEndRef = useRef(null);
  const stompClientRef = useRef(null);
  // Create session ID
  const sessionIdRef = useRef(
    sessionStorage.getItem("chatSessionId") ||
      (() => {
        const id = crypto.randomUUID();
        sessionStorage.setItem("chatSessionId", id);
        return id;
      })(),
  );

  // Connect WebSocket
  // useEffect(() => {
  //   const socket = new SockJS("http://localhost:8080/ws-chat");
  //   const stompClient = new Client({
  //     webSocketFactory: () => socket,
  //     reconnectDelay: 5000,
  //     debug: (str) => console.log("[STOMP]", str),
  //     onConnect: () => {
  //       console.log("Connected to WebSocket");

  //       // Subscribe to receive messages from the server
  //       stompClient.subscribe(
  //         `/topic/chat/${sessionIdRef.current}`,
  //         (message) => {
  //           const body = JSON.parse(message.body);

  //           setIsTyping(false);

  //           // Show AI text if present
  //           if (body.content) {
  //             setMessages((prev) => [
  //               ...prev,
  //               { sender: "ai", text: body.content },
  //             ]);
  //           }

  //           // Handle UI actions
  //           if (body.uiAction?.type === "NAVIGATE") {
  //             setTimeout(() => {
  //               navigate(body.uiAction.path);
  //             }, 500); // small delay so user sees message
  //           }
  //         }
  //       );

  //     },
  //     onStompError: (frame) => {
  //       console.error("Broker error:", frame.headers["message"]);
  //       // Clear typing indicator on error
  //       setIsTyping(false);
  //     },
  //   });

  //   stompClient.activate();
  //   stompClientRef.current = stompClient;

  //   return () => {
  //     stompClient.deactivate();
  //   };
  // }, []);

  // Auto-scroll when new message arrives or typing indicator changes
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  const toggleChat = () => setIsOpen(!isOpen);

  const handleSend = () => {
    if (!input.trim()) return;

    const message = {
      sessionId: sessionIdRef.current,
      sender: "User",
      content: input,
    };

    // Add user message immediately to the UI
    setMessages((prev) => [...prev, { sender: "user", text: input }]);

    // Show typing indicator (will remain visible even if user sends multiple messages)
    setIsTyping(true);

    // Send message to backend via STOMP
    if (stompClientRef.current && stompClientRef.current.connected) {
      stompClientRef.current.publish({
        destination: "/app/sendMessage",
        body: JSON.stringify(message),
      });
    } else {
      console.warn("STOMP client not connected.");
      // Clear typing indicator if send fails
      setIsTyping(false);
    }

    // Clear input field
    setInput("");
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") handleSend();
  };

  return (
    <div className="fixed bottom-5 right-5 z-50">
      {/* Floating Chat Button */}
      {!isOpen && (
        <button
          onClick={toggleChat}
          className="bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white p-4 rounded-full shadow-2xl transition-all duration-300 focus:outline-none focus:ring-4 focus:ring-red-300 hover:scale-110"
          aria-label="Open chat"
        >
          <Bot className="w-7 h-7" />
        </button>
      )}

      {/* Chat Window */}
      <div
        className={`${
          isOpen ? "scale-100 opacity-100" : "scale-0 opacity-0"
        } origin-bottom-right transition-all duration-300 ease-in-out`}
      >
        {isOpen && (
          <div className="w-80 sm:w-96 h-[500px] bg-white shadow-2xl rounded-3xl flex flex-col overflow-hidden border border-gray-200">
            {/* Header */}
            <div className="flex items-center justify-between px-5 py-4 bg-gradient-to-r from-red-600 to-red-700 text-white">
              <div className="flex items-center space-x-3">
                <div className="bg-white text-red-600 p-2 rounded-full shadow-md">
                  <Bot className="w-5 h-5" />
                </div>
                <div>
                  <h2 className="font-semibold text-base">AI Assistant</h2>
                  <p className="text-xs text-red-100">Always here to help</p>
                </div>
              </div>
              <button
                onClick={toggleChat}
                className="hover:bg-white/20 p-2 rounded-full focus:outline-none transition-colors"
                aria-label="Close chat"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Chat Messages - aria-live for screen reader announcements */}
            <div
              className="flex-1 overflow-y-auto p-4 space-y-3 bg-gradient-to-b from-gray-50 to-gray-100"
              aria-live="polite"
              aria-atomic="false"
            >
              {messages.map((msg, i) => (
                <div
                  key={i}
                  className={`flex ${
                    msg.sender.toLowerCase() === "user"
                      ? "justify-end"
                      : "justify-start"
                  } animate-fadeIn`}
                >
                  <div
                    className={`px-4 py-2.5 rounded-2xl text-sm max-w-[80%] shadow-sm ${
                      msg.sender.toLowerCase() === "user"
                        ? "bg-gradient-to-r from-red-600 to-red-700 text-white rounded-br-sm"
                        : "bg-white text-gray-800 rounded-bl-sm border border-gray-200"
                    }`}
                  >
                    {msg.sender.toLowerCase() === "user" ? (
                      msg.text
                    ) : (
                      <div className="prose prose-sm max-w-none text-gray-800">
                        <ReactMarkdown>{msg.text}</ReactMarkdown>
                      </div>
                    )}
                  </div>
                </div>
              ))}

              {/* Typing Indicator - only shown when waiting for AI response */}
              {isTyping && (
                <div
                  className="flex justify-start animate-fadeIn"
                  role="status"
                  aria-label="AI is typing"
                >
                  <div className="px-4 py-3 rounded-2xl rounded-bl-sm bg-white text-gray-800 shadow-sm border border-gray-200 flex items-center space-x-2">
                    <div className="flex space-x-1">
                      <span
                        className="w-2 h-2 bg-gray-400 rounded-full animate-extra-bounce"
                        style={{ animationDelay: "0ms" }}
                      ></span>
                      <span
                        className="w-2 h-2 bg-gray-400 rounded-full animate-extra-bounce"
                        style={{ animationDelay: "150ms" }}
                      ></span>
                      <span
                        className="w-2 h-2 bg-gray-400 rounded-full animate-extra-bounce"
                        style={{ animationDelay: "300ms" }}
                      ></span>
                    </div>
                  </div>
                </div>
              )}

              {/* Auto-scroll anchor */}
              <div ref={chatEndRef} />
            </div>

            {/* Input Box */}
            <div className="p-4 bg-white border-t border-gray-200">
              <div className="flex items-center space-x-2 bg-gray-100 rounded-full px-2 py-1">
                <input
                  type="text"
                  className="flex-1 px-3 py-2.5 text-sm bg-transparent focus:outline-none"
                  placeholder="Type your message..."
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  aria-label="Message input"
                />
                <button
                  onClick={handleSend}
                  disabled={!input.trim()}
                  className="bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 disabled:from-gray-300 disabled:to-gray-400 text-white p-2.5 rounded-full transition-all shadow-md hover:shadow-lg disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-red-400"
                  aria-label="Send message"
                >
                  <Send className="w-4 h-4 transform -translate-x-[1px] translate-y-[1px]" />
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      <style jsx>{`
        @keyframes fadeIn {
          from {
            opacity: 0;
            transform: translateY(10px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        .animate-fadeIn {
          animation: fadeIn 0.3s ease-out;
        }
      `}</style>
    </div>
  );
};

export default ChatBox;
