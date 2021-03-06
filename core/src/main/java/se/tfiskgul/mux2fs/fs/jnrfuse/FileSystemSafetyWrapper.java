/*
MIT License

Copyright (c) 2017 Carl-Frederik Hallberg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package se.tfiskgul.mux2fs.fs.jnrfuse;

import static se.tfiskgul.mux2fs.Constants.BUG;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.ffi.Pointer;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

/**
 * Wraps a NamedFileSystem to catch any Throwable.
 *
 * Returns -EIO and logs the exceptions.
 */
public final class FileSystemSafetyWrapper extends FuseStubFS {

	private static final Logger logger = LoggerFactory.getLogger(FileSystemSafetyWrapper.class);
	private final NamedJnrFuseFileSystem delegate;

	public FileSystemSafetyWrapper(NamedJnrFuseFileSystem delegate) {
		this.delegate = delegate;
	}

	@Override
	public String getFSName() {
		return delegate.getFSName();
	}

	@Override
	public void mount(Path mountPoint, boolean blocking, boolean debug, String[] fuseOpts) {
		super.mount(mountPoint, blocking, debug, fuseOpts);
	}

	@Override
	public int readdir(String path, Pointer buf, FuseFillDir filter, long offset, FuseFileInfo fi) {
		return wrap(() -> delegate.readdir(path, buf, filter, offset, fi));
	}

	@Override
	public int readlink(String path, Pointer buf, long size) {
		return wrap(() -> delegate.readlink(path, buf, size));
	}

	@Override
	public int getattr(String path, FileStat stat) {
		return wrap(() -> delegate.getattr(path, stat));
	}

	@Override
	public int open(String path, FuseFileInfo fi) {
		return wrap(() -> delegate.open(path, fi));
	}

	@Override
	public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
		return wrap(() -> delegate.read(path, buf, size, offset, fi));
	}

	@Override
	public int release(String path, FuseFileInfo fi) {
		return wrap(() -> delegate.release(path, fi));
	}

	@Override
	public void umount() {
		super.umount();
	}

	@Override
	public void destroy(Pointer initResult) {
		wrap(() -> delegate.destroy(initResult));
	}

	private int wrap(Supplier<Integer> supplier) {
		try {
			return supplier.get();
		} catch (Throwable e) {
			logger.error("BUG: Uncaught exception!", e);
			return BUG;
		}
	}

	private void wrap(Runnable runnable) {
		try {
			runnable.run();
		} catch (Throwable e) {
			logger.error("BUG: Uncaught exception!", e);
		}
	}
}
