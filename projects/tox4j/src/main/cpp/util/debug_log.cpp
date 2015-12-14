#include "util/debug_log.h"

#include <tox/core.h>

#include <cctype>
#include <cstdlib>
#include <iostream>
#include <map>

#include <jni.h>


/****************************************************************************
 *
 * :: Log and log entry.
 *
 ****************************************************************************/


// Global log.
JniLog jni_log;


struct JniLog::data
{
  int max_size = 100;
  std::recursive_mutex mutex;
  protolog::JniLog log;
};


JniLog::Entry::Entry (protolog::JniLogEntry *entry, std::unique_lock<std::recursive_mutex> lock)
  : entry (entry)
  , lock (std::move (lock))
{
}


JniLog::Entry::~Entry ()
{
}


JniLog::JniLog ()
  : self (std::make_unique<data> ())
{
}

JniLog::~JniLog ()
{
}


JniLog::Entry
JniLog::new_entry ()
{
  // No lock for max_size.
  if (self->max_size == 0)
    return { };

  // Acquire a lock and pass it to the Entry.
  std::unique_lock<std::recursive_mutex> lock (self->mutex);
  if (self->log.entries_size () >= self->max_size)
    // If the log is full, unlock right away and return null.
    return nullptr;
  return JniLog::Entry (self->log.add_entries (), std::move (lock));
}

std::vector<char>
JniLog::clear ()
{
  std::lock_guard<std::recursive_mutex> lock (self->mutex);
  std::vector<char> buffer (self->log.ByteSize ());
  self->log.SerializeToArray (buffer.data (), buffer.size ());
  self->log.Clear ();

  return buffer;
}

bool
JniLog::empty () const
{
  std::lock_guard<std::recursive_mutex> lock (self->mutex);
  return self->log.entries_size () == 0;
}

void
JniLog::max_size (int max_size)
{
  self->max_size = max_size;
}

int
JniLog::max_size () const
{
  return self->max_size;
}


/****************************************************************************
 *
 * :: Function name registry.
 *
 ****************************************************************************/


/**
 * The global singleton is in a function because otherwise static
 * initialisation order does not guarantee the map to be initialised when it
 * is accessed from another translation unit calling register_func.
 */
static std::map<uintptr_t, std::string const> &
func_names ()
{
  static std::map<uintptr_t, std::string const> func_names;
  return func_names;
}


bool
register_func (uintptr_t func, char const *name)
{
  auto &names = func_names ();
  assert (names.find (func) == names.end ());
  names.insert (std::make_pair (func, name));
  return true;
}


void
print_func (protolog::JniLogEntry &log_entry, uintptr_t func)
{
  auto &names = func_names ();
  auto found = names.find (func);
  if (found != names.end ())
    log_entry.set_name (found->second);
  else
    log_entry.set_name (std::to_string (func));
}


/****************************************************************************
 *
 * :: Common print_arg specialisations for C++ and JNI types.
 *
 ****************************************************************************/


template<typename Arg>
void
print_arg (protolog::Value &value, Arg arg)
{
  value.set_v_sint64 (arg);
}

template void print_arg<         bool     > (protolog::Value &,          bool     );
template void print_arg<  signed char     > (protolog::Value &,   signed char     );
template void print_arg<unsigned char     > (protolog::Value &, unsigned char     );
template void print_arg<  signed short    > (protolog::Value &,   signed short    );
template void print_arg<unsigned short    > (protolog::Value &, unsigned short    );
template void print_arg<  signed int      > (protolog::Value &,   signed int      );
template void print_arg<unsigned int      > (protolog::Value &, unsigned int      );
template void print_arg<  signed long     > (protolog::Value &,   signed long     );
template void print_arg<unsigned long     > (protolog::Value &, unsigned long     );
template void print_arg<  signed long long> (protolog::Value &,   signed long long);
template void print_arg<unsigned long long> (protolog::Value &, unsigned long long);

template<>
void
print_arg<char const *> (protolog::Value &value, char const *data)
{
  if (data != nullptr)
    value.set_v_string (data);
  else
    value.set_v_string ("<null>");
}

template<>
void
print_arg<uint8_t *> (protolog::Value &value, uint8_t *data)
{
  if (data != nullptr)
    value.set_v_string ("in data");
  else
    value.set_v_string ("<null>");
}

template<>
void
print_arg<uint8_t const *> (protolog::Value &value, uint8_t const *data)
{
  if (data != nullptr)
    value.set_v_string ("out data");
  else
    value.set_v_string ("<null>");
}

void
print_arg (protolog::Value &value, uint8_t const *data, std::size_t length)
{
  if (data != nullptr)
    value.set_v_bytes (data, length);
  else
    value.set_v_string ("<null>");
}

template<>
void
print_arg<JNIEnv *> (protolog::Value &value, JNIEnv *data)
{
  if (data != nullptr)
    value.set_v_string ("<JNIEnv>");
  else
    value.set_v_string ("<null>");
}

template<>
void
print_arg<jbyteArray> (protolog::Value &value, jbyteArray data)
{
  if (data != nullptr)
    value.set_v_string ("<jbyteArray>");
  else
    value.set_v_string ("<null>");
}

template<>
void
print_arg<jintArray> (protolog::Value &value, jintArray data)
{
  if (data != nullptr)
    value.set_v_string ("<jintArray>");
  else
    value.set_v_string ("<null>");
}
