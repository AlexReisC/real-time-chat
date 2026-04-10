import { createContext, useContext, useReducer, useCallback } from 'react';

const ChatContext = createContext(null);

const initialState = {
  rooms: [],
  privates: [],
  activeChat: null,   // { type: 'room'|'private', data: Room|Conversation }
  messages: [],
  members: [],
  pagination: { page: 0, totalPages: 1 },
};

function reducer(state, action) {
  switch (action.type) {
    case 'SET_ROOMS':
      return { ...state, rooms: action.rooms };

    case 'ADD_ROOM':
      return { ...state, rooms: [action.room, ...state.rooms] };

    case 'UPDATE_ROOM':
      return {
        ...state,
        rooms: state.rooms.map((r) => (r.id === action.room.id ? action.room : r)),
        activeChat:
          state.activeChat?.type === 'room' && state.activeChat.data.id === action.room.id
            ? { ...state.activeChat, data: action.room }
            : state.activeChat,
      };

    case 'REMOVE_ROOM':
      return {
        ...state,
        rooms: state.rooms.filter((r) => r.id !== action.roomId),
        activeChat:
          state.activeChat?.type === 'room' && state.activeChat.data.id === action.roomId
            ? null
            : state.activeChat,
      };

    case 'SET_PRIVATES':
      return { ...state, privates: action.privates };

    case 'REMOVE_PRIVATE':
      return {
        ...state,
        privates: state.privates.filter((p) => p.userId !== action.userId),
        activeChat:
          state.activeChat?.type === 'private' && state.activeChat.data.userId === action.userId
            ? null
            : state.activeChat,
      };

    case 'SET_ACTIVE_CHAT':
      return {
        ...state,
        activeChat: action.chat,
        messages: [],
        members: [],
        pagination: { page: 0, totalPages: 1 },
      };

    case 'SET_MESSAGES':
      return { ...state, messages: action.messages };

    case 'PREPEND_MESSAGES':
      // older messages loaded from history — prepend, avoid duplicates
      return {
        ...state,
        messages: [
          ...action.messages.filter(
            (m) => !state.messages.some((existing) => existing.id === m.id)
          ),
          ...state.messages,
        ],
        pagination: action.pagination,
      };

    case 'APPEND_MESSAGE': {
      // avoid duplicates from STOMP delivery
      if (state.messages.some((m) => m.id === action.message.id)) return state;
      return { ...state, messages: [...state.messages, action.message] };
    }

    case 'SET_MEMBERS':
      return { ...state, members: action.members };

    default:
      return state;
  }
}

export function ChatProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  const setRooms = useCallback((rooms) => dispatch({ type: 'SET_ROOMS', rooms }), []);
  const addRoom = useCallback((room) => dispatch({ type: 'ADD_ROOM', room }), []);
  const updateRoom = useCallback((room) => dispatch({ type: 'UPDATE_ROOM', room }), []);
  const removeRoom = useCallback((roomId) => dispatch({ type: 'REMOVE_ROOM', roomId }), []);

  const setPrivates = useCallback((privates) => dispatch({ type: 'SET_PRIVATES', privates }), []);
  const removePrivate = useCallback((userId) => dispatch({ type: 'REMOVE_PRIVATE', userId }), []);

  const setActiveChat = useCallback(
    (chat) => dispatch({ type: 'SET_ACTIVE_CHAT', chat }),
    []
  );

  const setMessages = useCallback(
    (messages) => dispatch({ type: 'SET_MESSAGES', messages }),
    []
  );

  const prependMessages = useCallback(
    (messages, pagination) => dispatch({ type: 'PREPEND_MESSAGES', messages, pagination }),
    []
  );

  const appendMessage = useCallback(
    (message) => dispatch({ type: 'APPEND_MESSAGE', message }),
    []
  );

  const setMembers = useCallback(
    (members) => dispatch({ type: 'SET_MEMBERS', members }),
    []
  );

  return (
    <ChatContext.Provider
      value={{
        ...state,
        setRooms,
        addRoom,
        updateRoom,
        removeRoom,
        setPrivates,
        removePrivate,
        setActiveChat,
        setMessages,
        prependMessages,
        appendMessage,
        setMembers,
      }}
    >
      {children}
    </ChatContext.Provider>
  );
}

export function useChat() {
  const ctx = useContext(ChatContext);
  if (!ctx) throw new Error('useChat must be used within ChatProvider');
  return ctx;
}
